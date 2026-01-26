-- =========================================================
-- Migration V71: Convert payment.payment_status from ENUM to VARCHAR
-- =========================================================
-- Purpose: Fix PostgreSQL ENUM type mismatch error with Hibernate
-- Context: payment.payment_status ENUM causes casting errors with @Enumerated(EnumType.STRING)
-- Solution: Convert to VARCHAR(50) with CHECK constraint for validation
--
-- Author: GYDI Development Team
-- Date: 2026-01-26
-- Issue: PostgreSQL ENUM compatibility with Hibernate JPA
-- ENUM values: 'PENDING', 'PROCESSING', 'SUCCESS', 'FAILED', 'CANCELED'
-- =========================================================

-- Step 1: Drop partial indexes that have WHERE clauses referencing payment_status ENUM
-- These indexes were created in V42
DROP INDEX IF EXISTS payment.idx_payment_booking_user_success;
DROP INDEX IF EXISTS payment.idx_payment_booking_pending;

-- Step 2: Drop triggers that use the payment_status column (prevent ALTER TYPE error)
DROP TRIGGER IF EXISTS trg_payment_booking_updated_at ON payment.payment_booking;
DROP TRIGGER IF EXISTS trg_payment_booking_paid_at ON payment.payment_booking;

-- Step 3: Drop functions that may reference the ENUM type
DROP FUNCTION IF EXISTS payment.update_payment_booking_updated_at() CASCADE;
DROP FUNCTION IF EXISTS payment.set_payment_booking_paid_at() CASCADE;

-- Step 4: Drop CHECK constraints that reference ENUM type
-- These constraints were created in V42 and compare payment_status with ENUM values
ALTER TABLE payment.payment_booking
    DROP CONSTRAINT IF EXISTS chk_payment_success_paid_at;
ALTER TABLE payment.payment_booking
    DROP CONSTRAINT IF EXISTS chk_payment_success_transaction_id;

-- Step 5: Remove default value (eliminates ENUM dependency)
ALTER TABLE payment.payment_booking
    ALTER COLUMN payment_status DROP DEFAULT;

-- Step 6: Convert column from ENUM to VARCHAR
ALTER TABLE payment.payment_booking
    ALTER COLUMN payment_status TYPE VARCHAR(50) USING payment_status::TEXT;

-- Step 7: Drop the ENUM type (now safe)
DROP TYPE IF EXISTS payment.payment_status;

-- Step 8: Add CHECK constraint for validation
ALTER TABLE payment.payment_booking
    ADD CONSTRAINT chk_payment_booking_payment_status
    CHECK (payment_status IN ('PENDING', 'PROCESSING', 'SUCCESS', 'FAILED', 'CANCELED'));

-- Step 9: Recreate CHECK constraints that were dropped in Step 4 (from V42) with VARCHAR
-- Ensures SUCCESS payments have required fields
ALTER TABLE payment.payment_booking
    ADD CONSTRAINT chk_payment_success_paid_at CHECK (
        (payment_status != 'SUCCESS')
        OR (payment_status = 'SUCCESS' AND paid_at IS NOT NULL)
    );

ALTER TABLE payment.payment_booking
    ADD CONSTRAINT chk_payment_success_transaction_id CHECK (
        (payment_status != 'SUCCESS')
        OR (payment_status = 'SUCCESS' AND transaction_id IS NOT NULL)
    );

-- Step 10: Re-add default value (as VARCHAR literal)
ALTER TABLE payment.payment_booking
    ALTER COLUMN payment_status SET DEFAULT 'PENDING';

-- Step 11: Add index for query performance (if column is frequently queried)
CREATE INDEX IF NOT EXISTS idx_payment_booking_payment_status
    ON payment.payment_booking(payment_status)
    WHERE payment_status IS NOT NULL;

-- Step 12: Recreate trigger functions to work with VARCHAR instead of ENUM
CREATE OR REPLACE FUNCTION payment.update_payment_booking_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION payment.set_payment_booking_paid_at()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.payment_status = 'SUCCESS' AND OLD.payment_status != 'SUCCESS' THEN
        NEW.paid_at = CURRENT_TIMESTAMP;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Step 13: Recreate triggers that were dropped in Step 2
CREATE TRIGGER trg_payment_booking_updated_at
    BEFORE UPDATE ON payment.payment_booking
    FOR EACH ROW
    EXECUTE FUNCTION payment.update_payment_booking_updated_at();

CREATE TRIGGER trg_payment_booking_paid_at
    BEFORE UPDATE ON payment.payment_booking
    FOR EACH ROW
    EXECUTE FUNCTION payment.set_payment_booking_paid_at();

-- Step 14: Recreate partial indexes that were dropped in Step 1
-- Now they work with VARCHAR instead of ENUM
CREATE INDEX idx_payment_booking_user_success
ON payment.payment_booking(user_id, payment_status, paid_at DESC)
WHERE payment_status = 'SUCCESS';

CREATE INDEX idx_payment_booking_pending
ON payment.payment_booking(payment_status, created_at)
WHERE payment_status IN ('PENDING', 'PROCESSING');

-- =========================================================
-- Rollback Plan:
-- =========================================================
-- DROP TRIGGER IF EXISTS trg_payment_booking_paid_at ON payment.payment_booking;
-- DROP TRIGGER IF EXISTS trg_payment_booking_updated_at ON payment.payment_booking;
-- DROP INDEX IF EXISTS idx_payment_booking_payment_status;
-- ALTER TABLE payment.payment_booking ALTER COLUMN payment_status DROP DEFAULT;
-- ALTER TABLE payment.payment_booking DROP CONSTRAINT IF EXISTS chk_payment_booking_payment_status;
-- CREATE TYPE payment.payment_status AS ENUM ('PENDING', 'PROCESSING', 'SUCCESS', 'FAILED', 'CANCELED');
-- ALTER TABLE payment.payment_booking
--     ALTER COLUMN payment_status TYPE payment.payment_status USING payment_status::payment.payment_status;
-- ALTER TABLE payment.payment_booking ALTER COLUMN payment_status SET DEFAULT 'PENDING'::payment.payment_status;
-- (Recreate triggers with ENUM)
-- =========================================================

COMMENT ON COLUMN payment.payment_booking.payment_status IS 'Payment status lifecycle: PENDING → PROCESSING → SUCCESS or FAILED, or CANCELED (converted from ENUM to VARCHAR for Hibernate compatibility)';
