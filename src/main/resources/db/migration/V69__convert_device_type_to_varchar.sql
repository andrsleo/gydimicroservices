-- =========================================================
-- Migration V69: Convert referrals.device_type from ENUM to VARCHAR
-- =========================================================
-- Purpose: Fix PostgreSQL ENUM type mismatch error with Hibernate
-- Context: referrals.device_type ENUM causes casting errors with @Enumerated(EnumType.STRING)
-- Solution: Convert to VARCHAR(50) with CHECK constraint for validation
--
-- Author: GYDI Development Team
-- Date: 2026-01-26
-- Issue: PostgreSQL ENUM compatibility with Hibernate JPA
-- ENUM values: 'DESKTOP', 'MOBILE', 'TABLET', 'BOT', 'UNKNOWN'
-- =========================================================

-- Step 1: Drop partial indexes that have WHERE clauses referencing device_type ENUM
-- This index was created in V31
DROP INDEX IF EXISTS referrals.idx_referral_clicks_bot_analysis;

-- Step 2: Drop views that depend on device_type column
-- This view was created in V33 and recreated in V34, uses FILTER (WHERE device_type = 'MOBILE')
DROP VIEW IF EXISTS referrals.v_click_analytics_30d CASCADE;

-- Step 3: Remove default value (eliminates ENUM dependency)
-- The column has DEFAULT 'UNKNOWN' which depends on the ENUM type
ALTER TABLE referrals.referral_clicks
    ALTER COLUMN device_type DROP DEFAULT;

-- Step 4: Convert column from ENUM to VARCHAR
ALTER TABLE referrals.referral_clicks
    ALTER COLUMN device_type TYPE VARCHAR(50) USING device_type::TEXT;

-- Step 5: Drop the ENUM type (now safe)
DROP TYPE IF EXISTS referrals.device_type;

-- Step 6: Add CHECK constraint for validation
ALTER TABLE referrals.referral_clicks
    ADD CONSTRAINT chk_referral_clicks_device_type
    CHECK (device_type IN ('DESKTOP', 'MOBILE', 'TABLET', 'BOT', 'UNKNOWN'));

-- Step 7: Re-add default value (as VARCHAR literal)
-- Restore the DEFAULT 'UNKNOWN' that existed before
ALTER TABLE referrals.referral_clicks
    ALTER COLUMN device_type SET DEFAULT 'UNKNOWN';

-- Step 8: Add index for query performance (if column is frequently queried)
CREATE INDEX IF NOT EXISTS idx_referral_clicks_device_type
    ON referrals.referral_clicks(device_type)
    WHERE device_type IS NOT NULL;

-- Step 9: Recreate partial index that was dropped in Step 1
-- Now it works with VARCHAR instead of ENUM
CREATE INDEX idx_referral_clicks_bot_analysis
ON referrals.referral_clicks(device_type, bot_score, clicked_at DESC)
WHERE device_type = 'BOT' OR bot_score > 70;

-- Step 10: Recreate the view that was dropped in Step 2
-- Now it works with VARCHAR instead of ENUM
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

-- =========================================================
-- Rollback Plan:
-- =========================================================
-- DROP INDEX IF EXISTS idx_referral_clicks_device_type;
-- ALTER TABLE referrals.referral_clicks DROP CONSTRAINT IF EXISTS chk_referral_clicks_device_type;
-- CREATE TYPE referrals.device_type AS ENUM ('DESKTOP', 'MOBILE', 'TABLET', 'BOT', 'UNKNOWN');
-- ALTER TABLE referrals.referral_clicks
--     ALTER COLUMN device_type TYPE referrals.device_type USING device_type::referrals.device_type;
-- =========================================================

COMMENT ON COLUMN referrals.referral_clicks.device_type IS 'Device type used for referral click: DESKTOP, MOBILE, TABLET, BOT, UNKNOWN (converted from ENUM to VARCHAR for Hibernate compatibility)';
