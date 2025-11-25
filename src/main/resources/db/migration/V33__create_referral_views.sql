-- V1.17__create_referral_views.sql
-- Purpose: Create materialized and regular views for analytics and reporting
-- Author: Database Architect AI
-- Date: 2025-11-12
-- Dependencies: V1.11, V1.12, V1.13, V1.14, V1.15, V1.16
-- Performance: Materialized views refresh periodically for fast dashboard queries

BEGIN;

-- =====================================================================
-- MATERIALIZED VIEW 1: AFFILIATE PERFORMANCE DASHBOARD
-- =====================================================================
-- Pre-aggregated stats for affiliate dashboard
-- Refresh: Every 15 minutes (balance between freshness and performance)

CREATE MATERIALIZED VIEW IF NOT EXISTS referrals.mv_affiliate_performance AS
SELECT
    rl.affiliate_id,
    -- Link statistics
    COUNT(DISTINCT rl.id) AS total_links_created,
    COUNT(DISTINCT rl.id) FILTER (WHERE rl.status = 'ACTIVE') AS active_links_count,
    COUNT(DISTINCT rl.id) FILTER (WHERE rl.status = 'EXPIRED') AS expired_links_count,

    -- Click statistics
    COALESCE(SUM(rl.clicks_count), 0) AS total_clicks,
    COALESCE(SUM(rl.clicks_count) FILTER (WHERE rl.created_at >= CURRENT_DATE - INTERVAL '30 days'), 0) AS clicks_last_30_days,
    COALESCE(SUM(rl.clicks_count) FILTER (WHERE rl.created_at >= CURRENT_DATE - INTERVAL '7 days'), 0) AS clicks_last_7_days,

    -- Conversion statistics
    COALESCE(SUM(rl.conversions_count), 0) AS total_conversions,
    COALESCE(SUM(rl.conversions_count) FILTER (WHERE rl.created_at >= CURRENT_DATE - INTERVAL '30 days'), 0) AS conversions_last_30_days,

    -- Conversion rate (%)
    CASE
        WHEN SUM(rl.clicks_count) > 0 THEN
            ROUND((SUM(rl.conversions_count)::DECIMAL / SUM(rl.clicks_count)::DECIMAL * 100), 2)
        ELSE 0
    END AS conversion_rate_percent,

    -- Commission earnings
    COALESCE(SUM(rl.total_commission), 0) AS total_earnings,
    COALESCE(SUM(cl.commission_amount) FILTER (WHERE cl.status = 'PENDING'), 0) AS pending_earnings,
    COALESCE(SUM(cl.commission_amount) FILTER (WHERE cl.status = 'APPROVED'), 0) AS approved_earnings,
    COALESCE(SUM(cl.commission_amount) FILTER (WHERE cl.status = 'PAID'), 0) AS paid_earnings,

    -- Best performing link
    (SELECT id FROM referrals.referral_links WHERE affiliate_id = rl.affiliate_id ORDER BY total_commission DESC LIMIT 1) AS top_link_id,
    (SELECT MAX(total_commission) FROM referrals.referral_links WHERE affiliate_id = rl.affiliate_id) AS top_link_earnings,

    -- Fraud risk score
    referrals.get_affiliate_fraud_risk(rl.affiliate_id) AS fraud_risk_score,

    -- Timestamps
    MIN(rl.created_at) AS first_link_created_at,
    MAX(rl.created_at) AS last_link_created_at,
    CURRENT_TIMESTAMP AS last_refreshed_at

FROM referrals.referral_links rl
LEFT JOIN referrals.commission_ledger cl ON cl.referral_link_id = rl.id
WHERE rl.deleted_at IS NULL
GROUP BY rl.affiliate_id;

-- Unique index for fast lookups
CREATE UNIQUE INDEX IF NOT EXISTS idx_mv_affiliate_performance_affiliate
ON referrals.mv_affiliate_performance(affiliate_id);

-- Additional indexes
CREATE INDEX IF NOT EXISTS idx_mv_affiliate_performance_earnings
ON referrals.mv_affiliate_performance(total_earnings DESC);

CREATE INDEX IF NOT EXISTS idx_mv_affiliate_performance_fraud
ON referrals.mv_affiliate_performance(fraud_risk_score DESC)
WHERE fraud_risk_score > 50;

-- =====================================================================
-- MATERIALIZED VIEW 2: FRAUD DETECTION METRICS
-- =====================================================================
-- Aggregated fraud metrics for admin dashboard
-- Refresh: Every 1 hour

CREATE MATERIALIZED VIEW IF NOT EXISTS referrals.mv_fraud_metrics AS
SELECT
    -- Alert statistics
    COUNT(*) AS total_alerts,
    COUNT(*) FILTER (WHERE status = 'PENDING') AS pending_alerts,
    COUNT(*) FILTER (WHERE status = 'INVESTIGATING') AS investigating_alerts,
    COUNT(*) FILTER (WHERE status = 'CONFIRMED') AS confirmed_frauds,
    COUNT(*) FILTER (WHERE status = 'DISMISSED') AS dismissed_alerts,

    -- Severity breakdown
    COUNT(*) FILTER (WHERE severity = 'CRITICAL') AS critical_alerts,
    COUNT(*) FILTER (WHERE severity = 'HIGH') AS high_alerts,
    COUNT(*) FILTER (WHERE severity = 'MEDIUM') AS medium_alerts,
    COUNT(*) FILTER (WHERE severity = 'LOW') AS low_alerts,

    -- Alert type distribution
    COUNT(*) FILTER (WHERE alert_type = 'SELF_REFERRAL') AS self_referral_count,
    COUNT(*) FILTER (WHERE alert_type = 'CLICK_FARM') AS click_farm_count,
    COUNT(*) FILTER (WHERE alert_type = 'BOT_TRAFFIC') AS bot_traffic_count,

    -- Average fraud score
    ROUND(AVG(fraud_score), 2) AS avg_fraud_score,
    MAX(fraud_score) AS max_fraud_score,

    -- Response times
    ROUND(AVG(EXTRACT(EPOCH FROM (investigated_at - created_at)) / 3600), 2) AS avg_investigation_time_hours,
    ROUND(AVG(EXTRACT(EPOCH FROM (resolved_at - created_at)) / 3600), 2) AS avg_resolution_time_hours,

    -- Time periods
    COUNT(*) FILTER (WHERE created_at >= CURRENT_DATE - INTERVAL '24 hours') AS alerts_last_24h,
    COUNT(*) FILTER (WHERE created_at >= CURRENT_DATE - INTERVAL '7 days') AS alerts_last_7_days,
    COUNT(*) FILTER (WHERE created_at >= CURRENT_DATE - INTERVAL '30 days') AS alerts_last_30_days,

    -- Top fraudulent affiliates
    (SELECT affiliate_id FROM referrals.fraud_alerts WHERE status = 'CONFIRMED' GROUP BY affiliate_id ORDER BY COUNT(*) DESC LIMIT 1) AS top_fraud_affiliate_id,

    CURRENT_TIMESTAMP AS last_refreshed_at

FROM referrals.fraud_alerts;

-- Index for fast refresh
CREATE INDEX IF NOT EXISTS idx_mv_fraud_metrics_refresh
ON referrals.mv_fraud_metrics(last_refreshed_at);

-- =====================================================================
-- REGULAR VIEW 1: TOP PERFORMING LINKS
-- =====================================================================
-- Real-time view of best performing links (no caching)

CREATE OR REPLACE VIEW referrals.v_top_links AS
SELECT
    rl.id AS link_id,
    rl.affiliate_id,
    rl.property_id,
    p.title AS property_title,
    rl.clicks_count,
    rl.conversions_count,
    rl.total_commission,
    CASE
        WHEN rl.clicks_count > 0 THEN
            ROUND((rl.conversions_count::DECIMAL / rl.clicks_count::DECIMAL * 100), 2)
        ELSE 0
    END AS conversion_rate_percent,
    rl.status,
    rl.created_at
FROM referrals.referral_links rl
JOIN properties.properties p ON p.id = rl.property_id
WHERE rl.deleted_at IS NULL
  AND rl.status = 'ACTIVE'
  AND rl.conversions_count > 0
ORDER BY rl.total_commission DESC
LIMIT 100;

-- =====================================================================
-- REGULAR VIEW 2: PENDING PAYOUTS
-- =====================================================================
-- Real-time view of commissions ready for payout

CREATE OR REPLACE VIEW referrals.v_pending_payouts AS
SELECT
    cl.affiliate_id,
    u.email AS affiliate_email,
    COALESCE(
        NULLIF(TRIM(CONCAT_WS(' ', up.first_name, up.last_name)), ''),
        u.email
    ) AS affiliate_name,
    COUNT(cl.id) AS commission_count,
    SUM(cl.commission_amount) AS total_payout_amount,
    MIN(cl.approved_at) AS earliest_approval_date,
    MAX(cl.approved_at) AS latest_approval_date,
    ARRAY_AGG(cl.id ORDER BY cl.approved_at) AS commission_ids
FROM referrals.commission_ledger cl
JOIN users.users u ON u.id = cl.affiliate_id
LEFT JOIN users.user_profile up ON up.user_id = u.id
WHERE cl.status = 'APPROVED'
GROUP BY cl.affiliate_id, u.email, up.first_name, up.last_name
HAVING SUM(cl.commission_amount) >= 50.00  -- Minimum payout threshold
ORDER BY SUM(cl.commission_amount) DESC;

-- =====================================================================
-- REGULAR VIEW 3: PROPERTY REFERRAL STATS
-- =====================================================================
-- Real-time view of property performance through referrals

CREATE OR REPLACE VIEW referrals.v_property_referral_stats AS
SELECT
    p.id AS property_id,
    p.title AS property_title,
    p.listing_type,
    COUNT(DISTINCT rl.id) AS total_referral_links,
    COUNT(DISTINCT rl.affiliate_id) AS unique_affiliates,
    SUM(rl.clicks_count) AS total_clicks,
    SUM(rl.conversions_count) AS total_conversions,
    CASE
        WHEN SUM(rl.clicks_count) > 0 THEN
            ROUND((SUM(rl.conversions_count)::DECIMAL / SUM(rl.clicks_count)::DECIMAL * 100), 2)
        ELSE 0
    END AS conversion_rate_percent,
    SUM(cl.commission_amount) AS total_commissions_paid,
    ROUND(AVG(cl.commission_rate) * 100, 2) AS avg_commission_rate_percent
FROM properties.properties p
LEFT JOIN referrals.referral_links rl ON rl.property_id = p.id AND rl.deleted_at IS NULL
LEFT JOIN referrals.commission_ledger cl ON cl.property_id = p.id
GROUP BY p.id, p.title, p.listing_type
HAVING COUNT(DISTINCT rl.id) > 0  -- Only properties with referral links
ORDER BY SUM(cl.commission_amount) DESC NULLS LAST;

-- =====================================================================
-- REGULAR VIEW 4: CLICK ANALYTICS (LAST 30 DAYS)
-- =====================================================================
-- Real-time view of recent click patterns for fraud detection

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

-- =====================================================================
-- REGULAR VIEW 5: AFFILIATE EARNINGS HISTORY
-- =====================================================================
-- Real-time view of all commissions for an affiliate (use with WHERE filter)

CREATE OR REPLACE VIEW referrals.v_affiliate_earnings_history AS
SELECT
    cl.id AS commission_id,
    cl.affiliate_id,
    cl.property_id,
    p.title AS property_title,
    cl.commission_rate,
    cl.commission_amount,
    cl.affiliate_plan,
    cl.status,
    cl.created_at,
    cl.approved_at,
    cl.paid_at,
    -- Calculate days until auto-approval (for PENDING status)
    CASE
        WHEN cl.status = 'PENDING' THEN
            30 - EXTRACT(DAY FROM (CURRENT_TIMESTAMP - cl.created_at))::INTEGER
        ELSE NULL
    END AS days_until_approval,
    -- Verification status
    referrals.verify_ledger_integrity(cl.id) AS integrity_verified
FROM referrals.commission_ledger cl
JOIN properties.properties p ON p.id = cl.property_id
ORDER BY cl.created_at DESC;

-- =====================================================================
-- REFRESH FUNCTIONS FOR MATERIALIZED VIEWS
-- =====================================================================

-- Function to refresh affiliate performance view
CREATE OR REPLACE FUNCTION referrals.refresh_affiliate_performance()
RETURNS void AS $$
BEGIN
    REFRESH MATERIALIZED VIEW CONCURRENTLY referrals.mv_affiliate_performance;
    RAISE NOTICE 'Refreshed mv_affiliate_performance at %', CURRENT_TIMESTAMP;
END;
$$ LANGUAGE plpgsql;

-- Function to refresh fraud metrics view
CREATE OR REPLACE FUNCTION referrals.refresh_fraud_metrics()
RETURNS void AS $$
BEGIN
    REFRESH MATERIALIZED VIEW CONCURRENTLY referrals.mv_fraud_metrics;
    RAISE NOTICE 'Refreshed mv_fraud_metrics at %', CURRENT_TIMESTAMP;
END;
$$ LANGUAGE plpgsql;

-- Function to refresh all materialized views
CREATE OR REPLACE FUNCTION referrals.refresh_all_materialized_views()
RETURNS void AS $$
BEGIN
    PERFORM referrals.refresh_affiliate_performance();
    PERFORM referrals.refresh_fraud_metrics();
    RAISE NOTICE 'Refreshed all materialized views at %', CURRENT_TIMESTAMP;
END;
$$ LANGUAGE plpgsql;

-- =====================================================================
-- COMMENTS FOR DOCUMENTATION
-- =====================================================================

COMMENT ON MATERIALIZED VIEW referrals.mv_affiliate_performance IS
'Pre-aggregated affiliate dashboard stats. Refresh every 15 minutes: SELECT referrals.refresh_affiliate_performance();';

COMMENT ON MATERIALIZED VIEW referrals.mv_fraud_metrics IS
'Aggregated fraud detection metrics. Refresh every hour: SELECT referrals.refresh_fraud_metrics();';

COMMENT ON VIEW referrals.v_top_links IS
'Real-time view of top 100 performing links by commission earnings. No caching.';

COMMENT ON VIEW referrals.v_pending_payouts IS
'Real-time view of affiliates with approved commissions ready for payout (≥$50 threshold).';

COMMENT ON VIEW referrals.v_property_referral_stats IS
'Real-time property performance through referral program. Shows which properties generate most commissions. Updated to remove booking_amount references.';

COMMENT ON VIEW referrals.v_click_analytics_30d IS
'Real-time click analytics for last 30 days. Used for fraud detection and pattern analysis.';

COMMENT ON VIEW referrals.v_affiliate_earnings_history IS
'Real-time commission history. Use with WHERE affiliate_id = ? filter. Includes integrity verification. Updated to remove booking references.';

COMMENT ON FUNCTION referrals.refresh_affiliate_performance IS
'Refreshes mv_affiliate_performance materialized view. Run every 15 minutes via cron.';

COMMENT ON FUNCTION referrals.refresh_fraud_metrics IS
'Refreshes mv_fraud_metrics materialized view. Run every hour via cron.';

COMMENT ON FUNCTION referrals.refresh_all_materialized_views IS
'Refreshes all materialized views. Run for maintenance or after bulk data changes.';

-- =====================================================================
-- GRANT PERMISSIONS
-- =====================================================================

GRANT SELECT ON referrals.mv_affiliate_performance TO andresvargas;
GRANT SELECT ON referrals.mv_fraud_metrics TO andresvargas;
GRANT SELECT ON referrals.v_top_links TO andresvargas;
GRANT SELECT ON referrals.v_pending_payouts TO andresvargas;
GRANT SELECT ON referrals.v_property_referral_stats TO andresvargas;
GRANT SELECT ON referrals.v_click_analytics_30d TO andresvargas;
GRANT SELECT ON referrals.v_affiliate_earnings_history TO andresvargas;
GRANT EXECUTE ON FUNCTION referrals.refresh_affiliate_performance TO andresvargas;
GRANT EXECUTE ON FUNCTION referrals.refresh_fraud_metrics TO andresvargas;
GRANT EXECUTE ON FUNCTION referrals.refresh_all_materialized_views TO andresvargas;

COMMIT;

-- =====================================================================
-- REFRESH SCHEDULE RECOMMENDATIONS
-- =====================================================================
/*
CRON JOBS (PostgreSQL pg_cron or application scheduler):

-- Every 15 minutes: Refresh affiliate performance
0,15,30,45 * * * * SELECT referrals.refresh_affiliate_performance();

-- Every hour: Refresh fraud metrics
0 * * * * SELECT referrals.refresh_fraud_metrics();

-- Every day at 3 AM: Full maintenance
0 3 * * * SELECT referrals.refresh_all_materialized_views();
0 3 * * * SELECT referrals.create_next_month_partition();
0 3 * * * SELECT referrals.drop_old_partitions();
0 3 * * * SELECT referrals.auto_approve_pending_commissions();

-- Every week: Analyze statistics
0 4 * * 0 ANALYZE referrals.referral_links;
0 4 * * 0 ANALYZE referrals.referral_clicks;
0 4 * * 0 ANALYZE referrals.commission_ledger;
0 4 * * 0 ANALYZE referrals.fraud_alerts;

USAGE EXAMPLES:

-- Dashboard: Get affiliate stats
SELECT * FROM referrals.mv_affiliate_performance WHERE affiliate_id = 123;

-- Admin: Get fraud overview
SELECT * FROM referrals.mv_fraud_metrics;

-- Marketing: Get top performing links
SELECT * FROM referrals.v_top_links LIMIT 10;

-- Finance: Get pending payouts
SELECT * FROM referrals.v_pending_payouts;

-- Analytics: Get property performance
SELECT * FROM referrals.v_property_referral_stats
WHERE total_conversions > 0
ORDER BY total_commissions_paid DESC;

-- Fraud detection: Get suspicious clicks
SELECT * FROM referrals.v_click_analytics_30d
WHERE bot_clicks > 10 OR vpn_clicks > 20;

-- Affiliate: Get earnings history
SELECT * FROM referrals.v_affiliate_earnings_history
WHERE affiliate_id = 123
ORDER BY created_at DESC;
*/

-- =====================================================================
-- ROLLBACK (FOR REFERENCE ONLY - NOT EXECUTED)
-- =====================================================================
-- DROP FUNCTION IF EXISTS referrals.refresh_all_materialized_views();
-- DROP FUNCTION IF EXISTS referrals.refresh_fraud_metrics();
-- DROP FUNCTION IF EXISTS referrals.refresh_affiliate_performance();
-- DROP VIEW IF EXISTS referrals.v_affiliate_earnings_history;
-- DROP VIEW IF EXISTS referrals.v_click_analytics_30d;
-- DROP VIEW IF EXISTS referrals.v_property_referral_stats;
-- DROP VIEW IF EXISTS referrals.v_pending_payouts;
-- DROP VIEW IF EXISTS referrals.v_top_links;
-- DROP MATERIALIZED VIEW IF EXISTS referrals.mv_fraud_metrics;
-- DROP MATERIALIZED VIEW IF EXISTS referrals.mv_affiliate_performance;