-- V86__rename_affiliate_commission_to_referral_commission.sql
-- Purpose: Rename commissions.affiliate_commission to commissions.referral_commission
-- This rename better reflects the domain language: commissions are paid to "referrers"
-- (affiliates who referred guests), not to "affiliates" in the generic sense.
-- Author: GYDI Development Team
-- Date: 2026-02-17

BEGIN;

-- ============================================================================
-- 1. RENAME TABLE
-- ============================================================================

ALTER TABLE commissions.affiliate_commission
    RENAME TO referral_commission;

-- ============================================================================
-- 2. RENAME INDEXES
-- ============================================================================

ALTER INDEX commissions.idx_affiliate_commission_booking
    RENAME TO idx_referral_commission_booking;

ALTER INDEX commissions.idx_affiliate_commission_affiliate_status
    RENAME TO idx_referral_commission_affiliate_status;

ALTER INDEX commissions.idx_affiliate_commission_status_created
    RENAME TO idx_referral_commission_status_created;

ALTER INDEX commissions.idx_affiliate_commission_payment_queue
    RENAME TO idx_referral_commission_payment_queue;

ALTER INDEX commissions.idx_affiliate_commission_dispute_period
    RENAME TO idx_referral_commission_dispute_period;

-- Index added in V85
ALTER INDEX IF EXISTS commissions.idx_affiliate_commission_waiting_host_charge
    RENAME TO idx_referral_commission_waiting_host_charge;

-- ============================================================================
-- 3. RENAME CONSTRAINTS
-- ============================================================================

ALTER TABLE commissions.referral_commission
    RENAME CONSTRAINT fk_affiliate_commission_booking TO fk_referral_commission_booking;

ALTER TABLE commissions.referral_commission
    RENAME CONSTRAINT fk_affiliate_commission_affiliate TO fk_referral_commission_affiliate;

ALTER TABLE commissions.referral_commission
    RENAME CONSTRAINT chk_affiliate_commission_rate TO chk_referral_commission_rate;

ALTER TABLE commissions.referral_commission
    RENAME CONSTRAINT chk_affiliate_commission_amounts TO chk_referral_commission_amounts;

ALTER TABLE commissions.referral_commission
    RENAME CONSTRAINT chk_affiliate_commission_calculation TO chk_referral_commission_calculation;

ALTER TABLE commissions.referral_commission
    RENAME CONSTRAINT chk_affiliate_commission_status TO chk_referral_commission_status;

ALTER TABLE commissions.referral_commission
    RENAME CONSTRAINT chk_affiliate_commission_currency TO chk_referral_commission_currency;

ALTER TABLE commissions.referral_commission
    RENAME CONSTRAINT chk_affiliate_commission_plan TO chk_referral_commission_plan;

ALTER TABLE commissions.referral_commission
    RENAME CONSTRAINT uq_affiliate_commission_booking TO uq_referral_commission_booking;

-- ============================================================================
-- 4. RENAME TRIGGER
-- ============================================================================

ALTER TRIGGER tr_affiliate_commission_updated_at
    ON commissions.referral_commission
    RENAME TO tr_referral_commission_updated_at;

-- ============================================================================
-- 5. RENAME HELPER FUNCTION
-- ============================================================================

ALTER FUNCTION commissions.get_affiliate_commission_summary(BIGINT, DATE, DATE)
    RENAME TO get_referral_commission_summary;

-- ============================================================================
-- 6. UPDATE TABLE COMMENTS
-- ============================================================================

COMMENT ON TABLE commissions.referral_commission IS
'Platform pays referrers (affiliates) for successful referrals. Paid in batches on 1st and 15th of each month after 7-day dispute period. Platform expense. Renamed from affiliate_commission in V86.';

COMMENT ON FUNCTION commissions.get_referral_commission_summary IS
'Get commission summary for referrer dashboard. Returns total bookings, amounts by status, and currency. Renamed from get_affiliate_commission_summary in V86.';

COMMIT;

-- ============================================================================
-- ROLLBACK PLAN
-- ============================================================================
-- BEGIN;
-- ALTER TABLE commissions.referral_commission RENAME TO affiliate_commission;
-- ALTER INDEX commissions.idx_referral_commission_booking RENAME TO idx_affiliate_commission_booking;
-- ALTER INDEX commissions.idx_referral_commission_affiliate_status RENAME TO idx_affiliate_commission_affiliate_status;
-- ALTER INDEX commissions.idx_referral_commission_status_created RENAME TO idx_affiliate_commission_status_created;
-- ALTER INDEX commissions.idx_referral_commission_payment_queue RENAME TO idx_affiliate_commission_payment_queue;
-- ALTER INDEX commissions.idx_referral_commission_dispute_period RENAME TO idx_affiliate_commission_dispute_period;
-- ALTER INDEX IF EXISTS commissions.idx_referral_commission_waiting_host_charge RENAME TO idx_affiliate_commission_waiting_host_charge;
-- ALTER TABLE commissions.affiliate_commission RENAME CONSTRAINT fk_referral_commission_booking TO fk_affiliate_commission_booking;
-- ALTER TABLE commissions.affiliate_commission RENAME CONSTRAINT fk_referral_commission_affiliate TO fk_affiliate_commission_affiliate;
-- ALTER TABLE commissions.affiliate_commission RENAME CONSTRAINT chk_referral_commission_rate TO chk_affiliate_commission_rate;
-- ALTER TABLE commissions.affiliate_commission RENAME CONSTRAINT chk_referral_commission_amounts TO chk_affiliate_commission_amounts;
-- ALTER TABLE commissions.affiliate_commission RENAME CONSTRAINT chk_referral_commission_calculation TO chk_affiliate_commission_calculation;
-- ALTER TABLE commissions.affiliate_commission RENAME CONSTRAINT chk_referral_commission_status TO chk_affiliate_commission_status;
-- ALTER TABLE commissions.affiliate_commission RENAME CONSTRAINT chk_referral_commission_currency TO chk_affiliate_commission_currency;
-- ALTER TABLE commissions.affiliate_commission RENAME CONSTRAINT chk_referral_commission_plan TO chk_affiliate_commission_plan;
-- ALTER TABLE commissions.affiliate_commission RENAME CONSTRAINT uq_referral_commission_booking TO uq_affiliate_commission_booking;
-- ALTER TRIGGER tr_referral_commission_updated_at ON commissions.affiliate_commission RENAME TO tr_affiliate_commission_updated_at;
-- ALTER FUNCTION commissions.get_referral_commission_summary(BIGINT, DATE, DATE) RENAME TO get_affiliate_commission_summary;
-- COMMIT;
