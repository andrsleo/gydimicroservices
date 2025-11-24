-- V37__cleanup_booking_references_from_indexes.sql
-- Purpose: Recreate indexes that previously included booking_amount column
-- Author: Database Architect AI
-- Date: 2025-11-18
-- Dependencies: V36 (booking columns removed)
-- Rationale: V31 originally created indexes with booking_amount. After V36 removes those columns,
--            we need to recreate affected indexes without those references.

BEGIN;

-- =====================================================================
-- STEP 1: DROP INDEXES THAT INCLUDED BOOKING_AMOUNT
-- =====================================================================

-- These indexes were created by V31 but reference columns dropped by V36
DROP INDEX IF EXISTS referrals.idx_commission_ledger_earnings_history;
DROP INDEX IF EXISTS referrals.idx_commission_ledger_plan_analytics;

-- =====================================================================
-- STEP 2: RECREATE INDEXES WITHOUT BOOKING REFERENCES
-- =====================================================================

-- Covering index for affiliate earnings history (without booking_amount)
CREATE INDEX idx_commission_ledger_earnings_history
ON referrals.commission_ledger(affiliate_id, created_at DESC)
INCLUDE (commission_amount, status, affiliate_plan, property_id);

COMMENT ON INDEX referrals.idx_commission_ledger_earnings_history IS
'Covering index for affiliate dashboard earnings history. Includes frequently accessed columns to avoid table lookup. Updated to remove booking_amount reference.';

-- Index for plan-based analytics (without booking_amount)
CREATE INDEX idx_commission_ledger_plan_analytics
ON referrals.commission_ledger(affiliate_plan, status, created_at DESC)
INCLUDE (commission_amount);

COMMENT ON INDEX referrals.idx_commission_ledger_plan_analytics IS
'Analytics index for commission reporting by subscription tier. Groups commissions by affiliate plan for performance analysis. Updated to remove booking_amount reference.';

-- =====================================================================
-- STEP 3: ANALYZE TABLE FOR QUERY PLANNER
-- =====================================================================

-- Update statistics for query planner to use new indexes efficiently
ANALYZE referrals.commission_ledger;

COMMIT;

-- =====================================================================
-- SUMMARY OF CHANGES
-- =====================================================================
/*
RECREATED INDEXES:
- idx_commission_ledger_earnings_history: Now includes only commission_amount (not booking_amount)
- idx_commission_ledger_plan_analytics: Now includes only commission_amount (not booking_amount)

IMPACT:
- Indexes are now consistent with V36 schema (no booking references)
- Query performance maintained for earnings and analytics queries
- All existing queries continue to work without modification

COMPATIBILITY:
- If database was migrated from scratch (V1→V37): V31 creates correct indexes, V37 is idempotent
- If database has old V31 indexes: V37 recreates them without booking_amount
*/

-- =====================================================================
-- ROLLBACK (FOR REFERENCE ONLY - NOT EXECUTED)
-- =====================================================================
-- DROP INDEX IF EXISTS referrals.idx_commission_ledger_earnings_history;
-- DROP INDEX IF EXISTS referrals.idx_commission_ledger_plan_analytics;
-- Then re-run V31 to restore original indexes (only if V36 is also rolled back)
