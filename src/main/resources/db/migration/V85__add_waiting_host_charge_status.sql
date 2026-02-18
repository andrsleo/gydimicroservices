-- V85__add_waiting_host_charge_status.sql
-- Purpose: Add WAITING_HOST_CHARGE to affiliate_commission status and WITHHELD to host_commission status
-- Business Rule: Affiliate commissions must wait for host charge confirmation before being eligible for payout.
--               The platform never pays what it has not collected.
-- Author: GYDI Development Team
-- Date: 2026-02-17

BEGIN;

-- ============================================================================
-- 1. UPDATE affiliate_commission: add WAITING_HOST_CHARGE status
-- ============================================================================

-- Drop existing CHECK constraint
ALTER TABLE commissions.affiliate_commission
    DROP CONSTRAINT IF EXISTS chk_affiliate_commission_status;

-- Add updated CHECK constraint including the new WAITING_HOST_CHARGE status
ALTER TABLE commissions.affiliate_commission
    ADD CONSTRAINT chk_affiliate_commission_status
    CHECK (status IN (
        'WAITING_HOST_CHARGE',  -- New: Created, waiting for host to be charged first
        'PENDING',              -- Legacy: In 7-day dispute protection period
        'APPROVED',             -- Ready to pay (host charged + dispute period ended)
        'PAID',                 -- Successfully paid to affiliate
        'CANCELLED',            -- Booking cancelled or disputed before payment
        'WITHHELD',             -- Payment held (host never paid or investigation needed)
        'DISPUTED'              -- Guest disputed the booking
    ));

-- Update the default status: new affiliate commissions start in WAITING_HOST_CHARGE
ALTER TABLE commissions.affiliate_commission
    ALTER COLUMN status SET DEFAULT 'WAITING_HOST_CHARGE';

-- ============================================================================
-- 2. UPDATE host_commission: add WITHHELD status
-- ============================================================================

-- Drop existing CHECK constraint
ALTER TABLE commissions.host_commission
    DROP CONSTRAINT IF EXISTS chk_host_commission_status;

-- Add updated CHECK constraint including WITHHELD
ALTER TABLE commissions.host_commission
    ADD CONSTRAINT chk_host_commission_status
    CHECK (status IN (
        'PENDING',      -- Not yet charged
        'PROCESSING',   -- Stripe charge in progress
        'CHARGED',      -- Successfully charged to host
        'FAILED',       -- Charge failed (will retry up to MAX_RETRY_ATTEMPTS)
        'REFUNDED',     -- Charge refunded due to dispute
        'DISPUTED',     -- Booking under dispute
        'WITHHELD'      -- Permanently withheld after max retries exceeded
    ));

-- ============================================================================
-- 3. ADD PERFORMANCE INDEX for WAITING_HOST_CHARGE queries
-- ============================================================================

-- Index to find affiliate commissions waiting for host charge
-- Used by ChargeHostCommissionUseCase after successful host charge
CREATE INDEX IF NOT EXISTS idx_affiliate_commission_waiting_host_charge
    ON commissions.affiliate_commission(booking_id, status)
    WHERE status = 'WAITING_HOST_CHARGE';

-- ============================================================================
-- 4. UPDATE helper function to include WAITING_HOST_CHARGE in summary
-- ============================================================================

-- Drop existing function first (required when changing return type in PostgreSQL)
DROP FUNCTION IF EXISTS commissions.get_affiliate_commission_summary(BIGINT, DATE, DATE);

CREATE OR REPLACE FUNCTION commissions.get_affiliate_commission_summary(
    p_affiliate_id BIGINT,
    p_start_date DATE DEFAULT NULL,
    p_end_date DATE DEFAULT NULL
)
RETURNS TABLE (
    total_bookings BIGINT,
    total_commission_amount NUMERIC,
    waiting_host_charge_amount NUMERIC,
    pending_amount NUMERIC,
    approved_amount NUMERIC,
    paid_amount NUMERIC,
    withheld_amount NUMERIC,
    currency VARCHAR(3)
) AS $$
BEGIN
    RETURN QUERY
    SELECT
        COUNT(*)::BIGINT AS total_bookings,
        COALESCE(SUM(commission_amount), 0) AS total_commission_amount,
        COALESCE(SUM(CASE WHEN status = 'WAITING_HOST_CHARGE' THEN commission_amount ELSE 0 END), 0) AS waiting_host_charge_amount,
        COALESCE(SUM(CASE WHEN status = 'PENDING' THEN commission_amount ELSE 0 END), 0) AS pending_amount,
        COALESCE(SUM(CASE WHEN status = 'APPROVED' THEN commission_amount ELSE 0 END), 0) AS approved_amount,
        COALESCE(SUM(CASE WHEN status = 'PAID' THEN commission_amount ELSE 0 END), 0) AS paid_amount,
        COALESCE(SUM(CASE WHEN status = 'WITHHELD' THEN commission_amount ELSE 0 END), 0) AS withheld_amount,
        MAX(ac.currency) AS currency
    FROM commissions.affiliate_commission ac
    WHERE ac.affiliate_id = p_affiliate_id
      AND (p_start_date IS NULL OR ac.created_at::DATE >= p_start_date)
      AND (p_end_date IS NULL OR ac.created_at::DATE <= p_end_date);
END;
$$ LANGUAGE plpgsql;

-- ============================================================================
-- 5. UPDATE COLUMN COMMENTS
-- ============================================================================

COMMENT ON COLUMN commissions.affiliate_commission.status IS
'WAITING_HOST_CHARGE: Created but waiting for host commission to be charged first (new default). PENDING: Legacy/in dispute period. APPROVED: Ready to pay (host charged + dispute period ended). PAID: Successfully paid. CANCELLED: Booking cancelled. WITHHELD: Payment held (host never paid or investigation). DISPUTED: Booking under dispute.';

COMMENT ON COLUMN commissions.host_commission.status IS
'PENDING: Awaiting charge. PROCESSING: Stripe charge in progress. CHARGED: Successfully charged. FAILED: Charge failed (will retry). REFUNDED: Charge refunded. DISPUTED: Booking under dispute. WITHHELD: Permanently withheld after max retries exceeded - requires manual intervention.';

COMMIT;

-- ============================================================================
-- ROLLBACK PLAN
-- ============================================================================
-- BEGIN;
-- ALTER TABLE commissions.affiliate_commission DROP CONSTRAINT IF EXISTS chk_affiliate_commission_status;
-- ALTER TABLE commissions.affiliate_commission ADD CONSTRAINT chk_affiliate_commission_status
--     CHECK (status IN ('PENDING', 'APPROVED', 'PAID', 'CANCELLED', 'WITHHELD', 'DISPUTED'));
-- ALTER TABLE commissions.affiliate_commission ALTER COLUMN status SET DEFAULT 'PENDING';
-- DROP INDEX IF EXISTS commissions.idx_affiliate_commission_waiting_host_charge;
-- ALTER TABLE commissions.host_commission DROP CONSTRAINT IF EXISTS chk_host_commission_status;
-- ALTER TABLE commissions.host_commission ADD CONSTRAINT chk_host_commission_status
--     CHECK (status IN ('PENDING', 'PROCESSING', 'CHARGED', 'FAILED', 'REFUNDED', 'DISPUTED'));
-- COMMIT;
