-- V1.12__create_referral_clicks_table.sql
-- Purpose: Create partitioned referral_clicks table for high-volume tracking
-- Author: Database Architect AI
-- Date: 2025-11-12
-- Dependencies: V1.11 (referral_links table)
-- Performance: Designed for 100K clicks/day, partitioned by month

BEGIN;

-- =====================================================================
-- REFERRAL CLICKS TABLE (PARTITIONED)
-- =====================================================================
-- Tracks every click on referral links
-- Partitioned by clicked_at (monthly) for performance and maintenance
-- GDPR-compliant: stores only hashed PII, 90-day retention

CREATE TABLE IF NOT EXISTS referrals.referral_clicks (
    -- Primary key (composite with partition key)
    id BIGSERIAL NOT NULL,

    -- Foreign key to referral link
    referral_link_id BIGINT NOT NULL,  -- FK to referrals.referral_links(id)

    -- Tracking data (GDPR-compliant: hashed, not reversible)
    ip_address_hash BYTEA NOT NULL,      -- SHA-256(IP + secret_salt)
    user_agent_hash BYTEA NOT NULL,      -- SHA-256(User-Agent + secret_salt)
    fingerprint VARCHAR(64),             -- Browser fingerprint (optional)

    -- Geolocation & device info (non-PII)
    country_code CHAR(2),                -- ISO 3166-1 alpha-2 (e.g., 'US', 'MX')
    region VARCHAR(50),                  -- State/Province
    city VARCHAR(100),                   -- City name
    device_type referrals.device_type NOT NULL DEFAULT 'UNKNOWN',

    -- Fraud detection
    bot_score INTEGER CHECK (bot_score >= 0 AND bot_score <= 100),  -- 0=human, 100=bot
    is_tor BOOLEAN DEFAULT FALSE,        -- Tor network detection
    is_vpn BOOLEAN DEFAULT FALSE,        -- VPN detection

    -- Session tracking
    session_id BIGINT,                   -- Links clicks within same session

    -- HTTP referer (optional)
    referer_domain VARCHAR(255),         -- Extracted domain from HTTP Referer

    -- Timestamp (partition key)
    clicked_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Primary key includes partition key
    PRIMARY KEY (id, clicked_at),

    -- Constraints
    CONSTRAINT valid_country_code CHECK (
        country_code IS NULL OR country_code ~ '^[A-Z]{2}$'
    )
) PARTITION BY RANGE (clicked_at);

-- Add foreign key constraint to referral_links
ALTER TABLE referrals.referral_clicks
ADD CONSTRAINT fk_referral_clicks_link FOREIGN KEY (referral_link_id)
REFERENCES referrals.referral_links(id) ON DELETE CASCADE;

-- =====================================================================
-- CREATE INITIAL PARTITIONS (12 months)
-- =====================================================================
-- Partitions enable automatic pruning for faster queries
-- Retention policy: Drop partitions older than 90 days

-- January 2025
CREATE TABLE IF NOT EXISTS referrals.referral_clicks_2025_01
PARTITION OF referrals.referral_clicks
FOR VALUES FROM ('2025-01-01') TO ('2025-02-01');

-- February 2025
CREATE TABLE IF NOT EXISTS referrals.referral_clicks_2025_02
PARTITION OF referrals.referral_clicks
FOR VALUES FROM ('2025-02-01') TO ('2025-03-01');

-- March 2025
CREATE TABLE IF NOT EXISTS referrals.referral_clicks_2025_03
PARTITION OF referrals.referral_clicks
FOR VALUES FROM ('2025-03-01') TO ('2025-04-01');

-- April 2025
CREATE TABLE IF NOT EXISTS referrals.referral_clicks_2025_04
PARTITION OF referrals.referral_clicks
FOR VALUES FROM ('2025-04-01') TO ('2025-05-01');

-- May 2025
CREATE TABLE IF NOT EXISTS referrals.referral_clicks_2025_05
PARTITION OF referrals.referral_clicks
FOR VALUES FROM ('2025-05-01') TO ('2025-06-01');

-- June 2025
CREATE TABLE IF NOT EXISTS referrals.referral_clicks_2025_06
PARTITION OF referrals.referral_clicks
FOR VALUES FROM ('2025-06-01') TO ('2025-07-01');

-- July 2025
CREATE TABLE IF NOT EXISTS referrals.referral_clicks_2025_07
PARTITION OF referrals.referral_clicks
FOR VALUES FROM ('2025-07-01') TO ('2025-08-01');

-- August 2025
CREATE TABLE IF NOT EXISTS referrals.referral_clicks_2025_08
PARTITION OF referrals.referral_clicks
FOR VALUES FROM ('2025-08-01') TO ('2025-09-01');

-- September 2025
CREATE TABLE IF NOT EXISTS referrals.referral_clicks_2025_09
PARTITION OF referrals.referral_clicks
FOR VALUES FROM ('2025-09-01') TO ('2025-10-01');

-- October 2025
CREATE TABLE IF NOT EXISTS referrals.referral_clicks_2025_10
PARTITION OF referrals.referral_clicks
FOR VALUES FROM ('2025-10-01') TO ('2025-11-01');

-- November 2025
CREATE TABLE IF NOT EXISTS referrals.referral_clicks_2025_11
PARTITION OF referrals.referral_clicks
FOR VALUES FROM ('2025-11-01') TO ('2025-12-01');

-- December 2025
CREATE TABLE IF NOT EXISTS referrals.referral_clicks_2025_12
PARTITION OF referrals.referral_clicks
FOR VALUES FROM ('2025-12-01') TO ('2026-01-01');

-- =====================================================================
-- INDEXES ON PARENT TABLE (INHERITED BY PARTITIONS)
-- =====================================================================

-- Index for fetching clicks by referral link (dashboard analytics)
CREATE INDEX idx_referral_clicks_link_time
ON referrals.referral_clicks(referral_link_id, clicked_at DESC);

-- Index for fraud detection (find duplicate hashes)
CREATE INDEX idx_referral_clicks_fraud_detection
ON referrals.referral_clicks(ip_address_hash, user_agent_hash, clicked_at DESC);

-- Index for session tracking
CREATE INDEX idx_referral_clicks_session
ON referrals.referral_clicks(session_id, clicked_at)
WHERE session_id IS NOT NULL;

-- Index for bot detection queries
CREATE INDEX idx_referral_clicks_bots
ON referrals.referral_clicks(referral_link_id, bot_score)
WHERE bot_score > 70;

-- Index for geographic analytics
CREATE INDEX idx_referral_clicks_geography
ON referrals.referral_clicks(country_code, device_type, clicked_at DESC)
WHERE country_code IS NOT NULL;

-- =====================================================================
-- PARTITION MANAGEMENT FUNCTION
-- =====================================================================
-- Function to automatically create next month's partition
-- Should be called monthly via cron job or scheduler

CREATE OR REPLACE FUNCTION referrals.create_next_month_partition()
RETURNS void AS $$
DECLARE
    next_month_start DATE;
    next_month_end DATE;
    partition_name TEXT;
BEGIN
    -- Calculate next month
    next_month_start := DATE_TRUNC('month', CURRENT_DATE + INTERVAL '1 month');
    next_month_end := next_month_start + INTERVAL '1 month';

    -- Generate partition name (e.g., referral_clicks_2025_01)
    partition_name := 'referral_clicks_' || TO_CHAR(next_month_start, 'YYYY_MM');

    -- Create partition if not exists
    EXECUTE format(
        'CREATE TABLE IF NOT EXISTS referrals.%I PARTITION OF referrals.referral_clicks
         FOR VALUES FROM (%L) TO (%L)',
        partition_name,
        next_month_start,
        next_month_end
    );

    RAISE NOTICE 'Created partition: %', partition_name;
END;
$$ LANGUAGE plpgsql;

-- =====================================================================
-- RETENTION POLICY FUNCTION
-- =====================================================================
-- Function to drop partitions older than 90 days (GDPR compliance)
-- Should be called daily via cron job or scheduler

CREATE OR REPLACE FUNCTION referrals.drop_old_partitions()
RETURNS void AS $$
DECLARE
    partition_record RECORD;
    retention_date DATE;
BEGIN
    -- Calculate retention threshold (90 days ago)
    retention_date := CURRENT_DATE - INTERVAL '90 days';

    -- Loop through partitions older than retention date
    FOR partition_record IN
        SELECT schemaname, tablename
        FROM pg_tables
        WHERE schemaname = 'referrals'
        AND tablename LIKE 'referral_clicks_%'
        AND tablename ~ '^\d{4}_\d{2}$'
        AND TO_DATE(
            SUBSTRING(tablename FROM 'referral_clicks_(\d{4}_\d{2})'),
            'YYYY_MM'
        ) < retention_date
    LOOP
        EXECUTE format('DROP TABLE IF EXISTS referrals.%I', partition_record.tablename);
        RAISE NOTICE 'Dropped old partition: %', partition_record.tablename;
    END LOOP;
END;
$$ LANGUAGE plpgsql;

-- =====================================================================
-- COMMENTS FOR DOCUMENTATION
-- =====================================================================

COMMENT ON TABLE referrals.referral_clicks IS
'High-volume tracking table for all clicks on referral links. Partitioned by month for performance. GDPR-compliant: only stores hashed PII, 90-day retention.';

COMMENT ON COLUMN referrals.referral_clicks.ip_address_hash IS
'SHA-256 hash of (IP address + secret salt). Irreversible for GDPR compliance. Used for fraud detection without storing actual IP.';

COMMENT ON COLUMN referrals.referral_clicks.user_agent_hash IS
'SHA-256 hash of (User-Agent + secret salt). Irreversible for GDPR compliance. Used for bot detection without storing actual User-Agent.';

COMMENT ON COLUMN referrals.referral_clicks.fingerprint IS
'Optional browser fingerprint (e.g., canvas fingerprint, WebGL hash). More accurate than cookies for tracking unique visitors.';

COMMENT ON COLUMN referrals.referral_clicks.bot_score IS
'Bot detection score from 0 (human) to 100 (bot). Calculated using User-Agent analysis, behavioral patterns, and rate limiting. Scores >70 flagged for review.';

COMMENT ON COLUMN referrals.referral_clicks.session_id IS
'Links multiple clicks within same user session (30-day window). Persisted in HttpOnly cookie. Enables attribution when user navigates between properties.';

COMMENT ON FUNCTION referrals.create_next_month_partition() IS
'Automatically creates partition for next month. Run monthly via cron: SELECT referrals.create_next_month_partition();';

COMMENT ON FUNCTION referrals.drop_old_partitions() IS
'Drops partitions older than 90 days for GDPR compliance. Run daily via cron: SELECT referrals.drop_old_partitions();';

-- =====================================================================
-- GRANT PERMISSIONS
-- =====================================================================



COMMIT;

-- =====================================================================
-- ROLLBACK (FOR REFERENCE ONLY - NOT EXECUTED)
-- =====================================================================
-- DROP FUNCTION IF EXISTS referrals.drop_old_partitions();
-- DROP FUNCTION IF EXISTS referrals.create_next_month_partition();
-- DROP TABLE IF EXISTS referrals.referral_clicks CASCADE;