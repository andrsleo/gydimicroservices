-- V40__fix_country_code_type.sql
-- Purpose: Fix country_code column type from CHAR(2) to VARCHAR(2)
-- Author: Database Architect AI
-- Date: 2025-11-13
-- Dependencies: V34 (referral_clicks table), V39 (views)
-- Reason: JPA expects VARCHAR for String fields, but table was created with CHAR

BEGIN;

-- =====================================================================
-- STEP 1: DROP DEPENDENT VIEW
-- =====================================================================
-- Must drop view that depends on country_code column before altering type

DROP VIEW IF EXISTS referrals.v_click_analytics_30d;

-- =====================================================================
-- STEP 2: FIX COUNTRY_CODE COLUMN TYPE
-- =====================================================================
-- Change from CHAR(2) to VARCHAR(2) to match JPA entity expectations

ALTER TABLE referrals.referral_clicks
ALTER COLUMN country_code TYPE VARCHAR(2);

COMMENT ON COLUMN referrals.referral_clicks.country_code IS
'ISO 3166-1 alpha-2 country code (e.g., ''US'', ''MX''). Changed from CHAR(2) to VARCHAR(2) for JPA compatibility.';

-- =====================================================================
-- STEP 3: RECREATE VIEW
-- =====================================================================
-- Recreate v_click_analytics_30d view with same definition

CREATE OR REPLACE VIEW referrals.v_click_analytics_30d AS
SELECT
    rc.referral_link_id,
    rl.affiliate_id,
    COUNT(*) AS click_count,
    COUNT(DISTINCT rc.ip_address_hash) AS unique_ips,
    COUNT(DISTINCT rc.user_agent_hash) AS unique_user_agents,
    COUNT(DISTINCT rc.fingerprint) AS unique_fingerprints,
    COUNT(*) FILTER (WHERE rc.device_type = 'MOBILE') AS mobile_clicks,
    COUNT(*) FILTER (WHERE rc.device_type = 'DESKTOP') AS desktop_clicks,
    COUNT(*) FILTER (WHERE rc.device_type = 'BOT') AS bot_clicks,
    COUNT(*) FILTER (WHERE rc.bot_score > 70) AS suspicious_clicks,
    COUNT(*) FILTER (WHERE rc.is_vpn = TRUE) AS vpn_clicks,
    COUNT(*) FILTER (WHERE rc.is_tor = TRUE) AS tor_clicks,
    COUNT(DISTINCT rc.country_code) AS unique_countries,
    MODE() WITHIN GROUP (ORDER BY rc.country_code) AS most_common_country,
    ROUND(AVG(rc.bot_score), 2) AS avg_bot_score,
    MIN(rc.clicked_at) AS first_click_at,
    MAX(rc.clicked_at) AS last_click_at
FROM referrals.referral_clicks rc
JOIN referrals.referral_links rl ON rl.id = rc.referral_link_id
WHERE rc.clicked_at >= CURRENT_DATE - INTERVAL '30 days'
GROUP BY rc.referral_link_id, rl.affiliate_id
ORDER BY COUNT(*) DESC;

COMMENT ON VIEW referrals.v_click_analytics_30d IS
'Real-time click analytics for last 30 days. Used for fraud detection and pattern analysis.';

-- Restore permissions
GRANT SELECT ON referrals.v_click_analytics_30d TO andresvargas;

COMMIT;