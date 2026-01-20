-- V1.14__create_fraud_alerts_table.sql
-- Purpose: Create fraud alerts table for manual review and automated detection
-- Author: Database Architect AI
-- Date: 2025-11-12
-- Dependencies: V1.11 (referral_links), V1.12 (referral_clicks), V1.13 (commission_ledger)
-- Compliance: Fraud detection and prevention system

BEGIN;

-- =====================================================================
-- FRAUD ALERT STATUS ENUM
-- =====================================================================

CREATE TYPE referrals.fraud_alert_status AS ENUM (
    'PENDING',      -- Awaiting manual review
    'INVESTIGATING', -- Under active investigation
    'CONFIRMED',    -- Fraud confirmed, action taken
    'DISMISSED',    -- False positive, no fraud
    'RESOLVED'      -- Issue resolved (e.g., affiliate banned, commission revoked)
);

-- =====================================================================
-- FRAUD SEVERITY ENUM
-- =====================================================================

CREATE TYPE referrals.fraud_severity AS ENUM (
    'LOW',          -- Minor suspicious activity (e.g., unusual pattern)
    'MEDIUM',       -- Moderate concern (e.g., multiple suspicious clicks)
    'HIGH',         -- Serious fraud indicators (e.g., click farm patterns)
    'CRITICAL'      -- Immediate action required (e.g., self-referral, bots)
);

-- =====================================================================
-- FRAUD ALERTS TABLE
-- =====================================================================
-- Stores all detected fraud alerts for manual review
-- Automated detection rules create alerts with evidence
-- Human reviewers investigate and take action

CREATE TABLE IF NOT EXISTS referrals.fraud_alerts (
    -- Primary key
    id BIGSERIAL PRIMARY KEY,

    -- Alert classification
    alert_type VARCHAR(50) NOT NULL,  -- 'SELF_REFERRAL', 'CLICK_FARM', 'BOT_TRAFFIC', etc.
    severity referrals.fraud_severity NOT NULL DEFAULT 'LOW',

    -- Fraud score (calculated by detection algorithm)
    fraud_score INTEGER NOT NULL CHECK (fraud_score >= 0 AND fraud_score <= 100),

    -- Related entities (nullable to support different alert types)
    affiliate_id BIGINT,              -- FK to users.users(id)
    referral_link_id BIGINT,            -- FK to referrals.referral_links(id)
    commission_ledger_id BIGINT,        -- FK to referrals.commission_ledger(id)

    -- Evidence (flexible JSONB structure)
    evidence JSONB NOT NULL,

    -- Detection metadata
    detected_by VARCHAR(50) NOT NULL DEFAULT 'SYSTEM',  -- 'SYSTEM', 'MANUAL', 'USER_REPORT'
    detection_rule VARCHAR(100),      -- Name of automated rule that triggered alert

    -- Review workflow
    status referrals.fraud_alert_status NOT NULL DEFAULT 'PENDING',
    assigned_to VARCHAR(100),         -- Username of admin investigating
    resolution_notes TEXT,            -- Admin notes on investigation outcome
    action_taken TEXT,                -- Description of action (e.g., "Banned affiliate", "Revoked commission")

    -- Timestamps
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    investigated_at TIMESTAMP WITH TIME ZONE,
    resolved_at TIMESTAMP WITH TIME ZONE,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Constraints
    CONSTRAINT valid_investigation_workflow CHECK (
        (status = 'PENDING' AND investigated_at IS NULL AND resolved_at IS NULL) OR
        (status = 'INVESTIGATING' AND investigated_at IS NOT NULL AND resolved_at IS NULL) OR
        (status IN ('CONFIRMED', 'DISMISSED', 'RESOLVED') AND investigated_at IS NOT NULL)
    )
);

-- =====================================================================
-- INDEXES FOR PERFORMANCE
-- =====================================================================

-- Index for admin dashboard (fetch unresolved alerts by severity)
CREATE INDEX idx_fraud_alerts_unresolved
ON referrals.fraud_alerts(severity DESC, created_at DESC)
WHERE status IN ('PENDING', 'INVESTIGATING');

-- Index for affiliate profile (fetch all alerts for affiliate)
CREATE INDEX idx_fraud_alerts_affiliate
ON referrals.fraud_alerts(affiliate_id, created_at DESC)
WHERE affiliate_id IS NOT NULL;

-- Index for alert type analytics
CREATE INDEX idx_fraud_alerts_type_severity
ON referrals.fraud_alerts(alert_type, severity, created_at DESC);

-- Index for assigned investigations (workload tracking)
CREATE INDEX idx_fraud_alerts_assigned
ON referrals.fraud_alerts(assigned_to, status, created_at)
WHERE assigned_to IS NOT NULL;

-- Index for fraud score queries (high-risk alerts)
CREATE INDEX idx_fraud_alerts_high_risk
ON referrals.fraud_alerts(fraud_score DESC, created_at DESC)
WHERE fraud_score >= 70;

-- GIN index for evidence JSONB queries
CREATE INDEX idx_fraud_alerts_evidence
ON referrals.fraud_alerts USING GIN (evidence);

-- =====================================================================
-- PREDEFINED ALERT TYPES (FOR DOCUMENTATION)
-- =====================================================================
/*
Alert Type Reference:

1. SELF_REFERRAL
   - Description: Affiliate used their own referral link to book
   - Severity: CRITICAL
   - Evidence: {booking_id, affiliate_id, booking_user_id}

2. CLICK_FARM
   - Description: Abnormal click patterns from similar IPs/User-Agents
   - Severity: HIGH
   - Evidence: {ip_pattern, click_count, time_window, similar_clicks[]}

3. BOT_TRAFFIC
   - Description: High bot scores on clicks
   - Severity: MEDIUM to HIGH
   - Evidence: {bot_score, user_agent, click_count, patterns[]}

4. DUPLICATE_CONVERSIONS
   - Description: Same user booked through multiple affiliate links
   - Severity: MEDIUM
   - Evidence: {user_id, booking_ids[], referral_links[]}

5. SUSPICIOUS_TIMING
   - Description: Booking immediately after click (no browsing)
   - Severity: LOW to MEDIUM
   - Evidence: {click_time, booking_time, duration_seconds}

6. VPN_TOR_ABUSE
   - Description: High volume of clicks from VPN/Tor networks
   - Severity: MEDIUM
   - Evidence: {vpn_clicks_count, tor_clicks_count, total_clicks}

7. RATE_LIMIT_EXCEEDED
   - Description: Too many link generations or clicks from same IP
   - Severity: LOW to MEDIUM
   - Evidence: {ip_hash, action_count, time_window, threshold}

8. REFUND_PATTERN
   - Description: High refund rate for affiliate's referrals
   - Severity: MEDIUM
   - Evidence: {refund_rate, refund_count, total_conversions}
*/

-- =====================================================================
-- FUNCTION: CREATE FRAUD ALERT
-- =====================================================================
-- Helper function to create fraud alerts from triggers or application code

CREATE OR REPLACE FUNCTION referrals.create_fraud_alert(
    p_alert_type VARCHAR,
    p_severity referrals.fraud_severity,
    p_fraud_score INTEGER,
    p_affiliate_id BIGINT DEFAULT NULL,
    p_referral_link_id BIGINT DEFAULT NULL,
    p_commission_ledger_id BIGINT DEFAULT NULL,
    p_evidence JSONB DEFAULT '{}'::JSONB,
    p_detection_rule VARCHAR DEFAULT NULL
) RETURNS BIGINT AS $$
DECLARE
    alert_id BIGINT;
BEGIN
    INSERT INTO referrals.fraud_alerts (
        alert_type,
        severity,
        fraud_score,
        affiliate_id,
        referral_link_id,
        commission_ledger_id,
        evidence,
        detection_rule
    ) VALUES (
        p_alert_type,
        p_severity,
        p_fraud_score,
        p_affiliate_id,
        p_referral_link_id,
        p_commission_ledger_id,
        p_evidence,
        p_detection_rule
    ) RETURNING id INTO alert_id;

    RAISE NOTICE 'Created fraud alert: % (severity: %, score: %)',
        p_alert_type, p_severity, p_fraud_score;

    RETURN alert_id;
END;
$$ LANGUAGE plpgsql;

-- =====================================================================
-- FUNCTION: GET AFFILIATE FRAUD RISK SCORE
-- =====================================================================
-- Calculates aggregate fraud risk score for an affiliate (0-100)
-- Based on number and severity of alerts

CREATE OR REPLACE FUNCTION referrals.get_affiliate_fraud_risk(
    p_affiliate_id BIGINT
) RETURNS INTEGER AS $$
DECLARE
    alert_count INTEGER;
    critical_count INTEGER;
    high_count INTEGER;
    medium_count INTEGER;
    risk_score INTEGER;
BEGIN
    -- Count alerts by severity
    SELECT
        COUNT(*),
        COUNT(*) FILTER (WHERE severity = 'CRITICAL'),
        COUNT(*) FILTER (WHERE severity = 'HIGH'),
        COUNT(*) FILTER (WHERE severity = 'MEDIUM')
    INTO alert_count, critical_count, high_count, medium_count
    FROM referrals.fraud_alerts
    WHERE affiliate_id = p_affiliate_id
    AND status IN ('PENDING', 'INVESTIGATING', 'CONFIRMED')
    AND created_at > CURRENT_TIMESTAMP - INTERVAL '90 days';

    -- Calculate weighted risk score
    risk_score := LEAST(100,
        (critical_count * 50) +   -- Each critical alert adds 50 points
        (high_count * 25) +       -- Each high alert adds 25 points
        (medium_count * 10)       -- Each medium alert adds 10 points
    );

    RETURN risk_score;
END;
$$ LANGUAGE plpgsql;

-- =====================================================================
-- COMMENTS FOR DOCUMENTATION
-- =====================================================================

COMMENT ON TABLE referrals.fraud_alerts IS
'Fraud detection and manual review system. Automated rules create alerts with evidence. Human reviewers investigate and take action.';

COMMENT ON COLUMN referrals.fraud_alerts.alert_type IS
'Type of fraud detected (e.g., SELF_REFERRAL, CLICK_FARM, BOT_TRAFFIC). See predefined alert types in migration file.';

COMMENT ON COLUMN referrals.fraud_alerts.fraud_score IS
'Calculated fraud probability from 0 (no fraud) to 100 (certain fraud). Scores ≥70 require immediate review.';

COMMENT ON COLUMN referrals.fraud_alerts.evidence IS
'JSONB containing all evidence collected by detection algorithm. Structure varies by alert_type. Queryable with GIN index.';

COMMENT ON COLUMN referrals.fraud_alerts.detection_rule IS
'Name of automated rule that triggered alert (e.g., "self_referral_trigger", "rate_limit_exceeded"). NULL for manual reports.';

COMMENT ON COLUMN referrals.fraud_alerts.assigned_to IS
'Admin username assigned to investigate alert. NULL if unassigned. Used for workload distribution.';

COMMENT ON COLUMN referrals.fraud_alerts.action_taken IS
'Description of action taken after investigation (e.g., "Banned affiliate", "Revoked $150 commission", "No action - false positive").';

COMMENT ON FUNCTION referrals.create_fraud_alert IS
'Helper function to create fraud alerts. Used by triggers and application code. Returns created alert ID.';

COMMENT ON FUNCTION referrals.get_affiliate_fraud_risk IS
'Calculates aggregate fraud risk score (0-100) for affiliate based on recent alerts. Used for affiliate reputation scoring.';

-- =====================================================================
-- GRANT PERMISSIONS
-- =====================================================================



COMMIT;

-- =====================================================================
-- ROLLBACK (FOR REFERENCE ONLY - NOT EXECUTED)
-- =====================================================================
-- DROP FUNCTION IF EXISTS referrals.get_affiliate_fraud_risk(BIGINT);
-- DROP FUNCTION IF EXISTS referrals.create_fraud_alert;
-- DROP TABLE IF EXISTS referrals.fraud_alerts CASCADE;
-- DROP TYPE IF EXISTS referrals.fraud_severity CASCADE;
-- DROP TYPE IF EXISTS referrals.fraud_alert_status CASCADE;
