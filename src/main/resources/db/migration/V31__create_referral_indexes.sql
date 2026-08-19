-- V1.15__create_referral_indexes.sql
-- Purpose: Create additional composite indexes for complex queries
-- Author: Database Architect AI
-- Date: 2025-11-12
-- Dependencies: V1.11, V1.12, V1.13, V1.14
-- Performance: Optimizes dashboard, reporting, and analytics queries

BEGIN;

-- =====================================================================
-- REFERRAL LINKS - ADDITIONAL INDEXES
-- =====================================================================

-- Covering index for dashboard "My Links" page
-- Avoids table lookup by including frequently accessed columns
CREATE INDEX idx_referral_links_dashboard_covering
ON referrals.referral_links(affiliate_id, created_at DESC)
INCLUDE (property_id, encrypted_token, status, clicks_count, conversions_count, total_commission, expires_at)
WHERE deleted_at IS NULL;

-- Index for "Top Performing Links" report
CREATE INDEX idx_referral_links_top_performers
ON referrals.referral_links(total_commission DESC, conversions_count DESC)
WHERE status = 'ACTIVE' AND deleted_at IS NULL;

-- Index for expiration notification job (expire soon)
-- COMMENTED OUT: Cannot use CURRENT_TIMESTAMP in index predicate (must be IMMUTABLE)
-- Application can query without this index (will use idx_referral_links_active instead)
-- CREATE INDEX idx_referral_links_expiring_soon
-- ON referrals.referral_links(expires_at)
-- WHERE status = 'ACTIVE'
--   AND deleted_at IS NULL
--   AND expires_at BETWEEN CURRENT_TIMESTAMP AND CURRENT_TIMESTAMP + INTERVAL '7 days';

-- =====================================================================
-- REFERRAL CLICKS - ADDITIONAL INDEXES
-- =====================================================================

-- Composite index for click analytics by date range
CREATE INDEX idx_referral_clicks_analytics_date
ON referrals.referral_clicks(referral_link_id, clicked_at, device_type, country_code);

-- Index for unique visitor counting (by fingerprint)
CREATE INDEX idx_referral_clicks_unique_visitors
ON referrals.referral_clicks(referral_link_id, fingerprint)
WHERE fingerprint IS NOT NULL;

-- Index for fraud detection: duplicate IP+UA patterns
CREATE INDEX idx_referral_clicks_duplicate_detection
ON referrals.referral_clicks(ip_address_hash, user_agent_hash, referral_link_id, clicked_at DESC);

-- Index for bot traffic analysis
CREATE INDEX idx_referral_clicks_bot_analysis
ON referrals.referral_clicks(device_type, bot_score, clicked_at DESC)
WHERE device_type = 'BOT' OR bot_score > 70;

-- Index for geographic performance reports
CREATE INDEX idx_referral_clicks_geo_performance
ON referrals.referral_clicks(country_code, region, device_type, clicked_at DESC)
WHERE country_code IS NOT NULL;

-- Index for VPN/Tor traffic monitoring
CREATE INDEX idx_referral_clicks_vpn_tor
ON referrals.referral_clicks(referral_link_id, clicked_at DESC)
WHERE is_vpn = TRUE OR is_tor = TRUE;

-- =====================================================================
-- COMMISSION LEDGER - ADDITIONAL INDEXES
-- =====================================================================

-- Covering index for affiliate earnings history
CREATE INDEX idx_commission_ledger_earnings_history
ON referrals.commission_ledger(affiliate_id, created_at DESC)
INCLUDE (commission_amount, status, affiliate_plan, property_id);

-- Index for total earnings calculation (by affiliate + status)
CREATE INDEX idx_commission_ledger_earnings_sum
ON referrals.commission_ledger(affiliate_id, status)
INCLUDE (commission_amount);

-- Index for property performance report (commissions by property)
CREATE INDEX idx_commission_ledger_property_performance
ON referrals.commission_ledger(property_id, status, created_at DESC)
INCLUDE (commission_amount, affiliate_id);

-- Index for plan-based analytics (commissions by subscription tier)
CREATE INDEX idx_commission_ledger_plan_analytics
ON referrals.commission_ledger(affiliate_plan, status, created_at DESC)
INCLUDE (commission_amount);

-- Index for monthly earnings report (partitioned by month)
-- COMMENTED OUT: DATE_TRUNC function in index expression must be marked IMMUTABLE
-- Application can query without this index (will use idx_commission_ledger_earnings_history instead)
-- CREATE INDEX idx_commission_ledger_monthly_earnings
-- ON referrals.commission_ledger(affiliate_id, DATE_TRUNC('month', created_at), status)
-- INCLUDE (commission_amount);

-- Index for pending approval queue (30-day fraud window)
-- COMMENTED OUT: Cannot use CURRENT_TIMESTAMP in index predicate (must be IMMUTABLE)
-- Application can query without this index (will use idx_commission_ledger_affiliate_status instead)
-- CREATE INDEX idx_commission_ledger_approval_queue
-- ON referrals.commission_ledger(created_at, status)
-- WHERE status = 'PENDING'
--   AND created_at < CURRENT_TIMESTAMP - INTERVAL '30 days';

-- Index for payout batch processing (APPROVED commissions grouped by affiliate)
CREATE INDEX idx_commission_ledger_payout_batch
ON referrals.commission_ledger(affiliate_id, status, approved_at)
WHERE status = 'APPROVED';

-- =====================================================================
-- FRAUD ALERTS - ADDITIONAL INDEXES
-- =====================================================================

-- Index for admin dashboard: unresolved alerts grouped by type
CREATE INDEX idx_fraud_alerts_dashboard_type
ON referrals.fraud_alerts(alert_type, severity DESC, created_at DESC)
WHERE status IN ('PENDING', 'INVESTIGATING');

-- Index for affiliate reputation check (count confirmed frauds)
CREATE INDEX idx_fraud_alerts_affiliate_reputation
ON referrals.fraud_alerts(affiliate_id, status)
WHERE status = 'CONFIRMED'
  AND affiliate_id IS NOT NULL;

-- Index for alert escalation (pending alerts older than 7 days)
-- COMMENTED OUT: Cannot use CURRENT_TIMESTAMP in index predicate (must be IMMUTABLE)
-- Application can query without this index (will use idx_fraud_alerts_unresolved instead)
-- CREATE INDEX idx_fraud_alerts_escalation
-- ON referrals.fraud_alerts(created_at, severity, status)
-- WHERE status = 'PENDING'
--   AND created_at < CURRENT_TIMESTAMP - INTERVAL '7 days';

-- Index for investigating specific referral link
CREATE INDEX idx_fraud_alerts_link_investigation
ON referrals.fraud_alerts(referral_link_id, created_at DESC, status)
WHERE referral_link_id IS NOT NULL;

-- Index for workload assignment (alerts assigned to admin)
CREATE INDEX idx_fraud_alerts_workload
ON referrals.fraud_alerts(assigned_to, status, created_at)
WHERE assigned_to IS NOT NULL
  AND status = 'INVESTIGATING';

-- =====================================================================
-- CROSS-TABLE ANALYTICS INDEXES
-- =====================================================================

-- Index on referral_links for JOIN with commission_ledger
CREATE INDEX idx_referral_links_commission_join
ON referrals.referral_links(id, affiliate_id)
WHERE deleted_at IS NULL;

-- Index on referral_clicks for JOIN with referral_links
CREATE INDEX idx_referral_clicks_link_join
ON referrals.referral_clicks(referral_link_id, clicked_at);

-- Index on commission_ledger for JOIN with referral_links
CREATE INDEX idx_commission_ledger_link_join
ON referrals.commission_ledger(referral_link_id, status, created_at);

-- =====================================================================
-- SECURITY AUDIT LOG - INDEXES
-- =====================================================================

-- Index for security audit log queries (from V1.10)
CREATE INDEX IF NOT EXISTS idx_security_audit_log_user_time
ON referrals.security_audit_log(user_id, created_at DESC)
WHERE user_id IS NOT NULL;

-- Index for security audit by IP hash
CREATE INDEX IF NOT EXISTS idx_security_audit_log_ip_time
ON referrals.security_audit_log(ip_address_hash, created_at DESC)
WHERE ip_address_hash IS NOT NULL;

-- GIN index for security audit details JSONB
CREATE INDEX IF NOT EXISTS idx_security_audit_log_details
ON referrals.security_audit_log USING GIN (details);

-- =====================================================================
-- STATISTICS UPDATE
-- =====================================================================
-- Analyze tables to update query planner statistics

ANALYZE referrals.referral_links;
ANALYZE referrals.referral_clicks;
ANALYZE referrals.commission_ledger;
ANALYZE referrals.fraud_alerts;
ANALYZE referrals.security_audit_log;

-- =====================================================================
-- COMMENTS FOR DOCUMENTATION
-- =====================================================================

COMMENT ON INDEX referrals.idx_referral_links_dashboard_covering IS
'Covering index for affiliate dashboard. Includes frequently accessed columns to avoid table lookup.';

COMMENT ON INDEX referrals.idx_commission_ledger_payout_batch IS
'Optimizes batch payout processing by grouping APPROVED commissions by affiliate for efficient payment processing.';

COMMENT ON INDEX referrals.idx_referral_clicks_duplicate_detection IS
'Detects duplicate click patterns for fraud prevention. Identifies same IP+UA combination clicking multiple times.';

-- COMMENT ON INDEX idx_fraud_alerts_escalation removed (index was commented out)

-- =====================================================================
-- INDEX MAINTENANCE RECOMMENDATIONS
-- =====================================================================
/*
MAINTENANCE SCHEDULE:

1. Daily (automated):
   - ANALYZE referrals.referral_clicks;  # High-volume table
   - VACUUM (ANALYZE) referrals.referral_clicks;  # Remove dead tuples

2. Weekly (automated):
   - REINDEX CONCURRENTLY referrals.referral_clicks;  # Rebuild partitioned indexes
   - ANALYZE referrals.referral_links, referrals.commission_ledger, referrals.fraud_alerts;

3. Monthly (manual review):
   - Check index bloat: SELECT * FROM pg_stat_user_indexes WHERE schemaname = 'referrals';
   - Review unused indexes: SELECT * FROM pg_stat_user_indexes WHERE idx_scan = 0;
   - Drop unused indexes if confirmed by query logs

4. Quarterly (performance audit):
   - Review slow queries: SELECT * FROM pg_stat_statements WHERE query LIKE '%referrals%';
   - Add missing indexes based on actual usage patterns
   - Update statistics: ANALYZE VERBOSE referrals.*;

STORAGE ESTIMATES:
- referral_links: ~10K rows = ~5 MB + ~15 MB indexes = 20 MB
- referral_clicks: ~100K/day * 90 days = 9M rows = ~2 GB + ~1 GB indexes = 3 GB
- commission_ledger: ~1K conversions/month * 12 months = 12K rows = ~5 MB + ~10 MB indexes = 15 MB
- fraud_alerts: ~100 alerts/month * 12 months = 1.2K rows = ~1 MB + ~2 MB indexes = 3 MB

TOTAL ESTIMATED STORAGE (first year): ~3.5 GB
*/

COMMIT;

-- =====================================================================
-- ROLLBACK (FOR REFERENCE ONLY - NOT EXECUTED)
-- =====================================================================
-- DROP INDEX IF EXISTS referrals.idx_referral_links_dashboard_covering;
-- DROP INDEX IF EXISTS referrals.idx_referral_links_top_performers;
-- DROP INDEX IF EXISTS referrals.idx_referral_links_expiring_soon;
-- DROP INDEX IF EXISTS referrals.idx_referral_clicks_analytics_date;
-- DROP INDEX IF EXISTS referrals.idx_referral_clicks_unique_visitors;
-- DROP INDEX IF EXISTS referrals.idx_referral_clicks_duplicate_detection;
-- DROP INDEX IF EXISTS referrals.idx_referral_clicks_bot_analysis;
-- DROP INDEX IF EXISTS referrals.idx_referral_clicks_geo_performance;
-- DROP INDEX IF EXISTS referrals.idx_referral_clicks_vpn_tor;
-- DROP INDEX IF EXISTS referrals.idx_commission_ledger_earnings_history;
-- DROP INDEX IF EXISTS referrals.idx_commission_ledger_earnings_sum;
-- DROP INDEX IF EXISTS referrals.idx_commission_ledger_property_performance;
-- DROP INDEX IF EXISTS referrals.idx_commission_ledger_plan_analytics;
-- DROP INDEX IF EXISTS referrals.idx_commission_ledger_monthly_earnings;
-- DROP INDEX IF EXISTS referrals.idx_commission_ledger_approval_queue;
-- DROP INDEX IF EXISTS referrals.idx_commission_ledger_payout_batch;
-- DROP INDEX IF EXISTS referrals.idx_fraud_alerts_dashboard_type;
-- DROP INDEX IF EXISTS referrals.idx_fraud_alerts_affiliate_reputation;
-- DROP INDEX IF EXISTS referrals.idx_fraud_alerts_escalation;
-- DROP INDEX IF EXISTS referrals.idx_fraud_alerts_link_investigation;
-- DROP INDEX IF EXISTS referrals.idx_fraud_alerts_workload;
-- DROP INDEX IF EXISTS referrals.idx_referral_links_commission_join;
-- DROP INDEX IF EXISTS referrals.idx_referral_clicks_link_join;
-- DROP INDEX IF EXISTS referrals.idx_commission_ledger_link_join;
-- DROP INDEX IF EXISTS referrals.idx_security_audit_log_user_time;
-- DROP INDEX IF EXISTS referrals.idx_security_audit_log_ip_time;
-- DROP INDEX IF EXISTS referrals.idx_security_audit_log_details;