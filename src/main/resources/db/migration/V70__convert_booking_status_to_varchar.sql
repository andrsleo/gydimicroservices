-- =========================================================
-- Migration V70: Convert referrals.booking_status from ENUM to VARCHAR
-- =========================================================
-- Purpose: Fix PostgreSQL ENUM type mismatch error with Hibernate
-- Context: referrals.booking_status ENUM causes casting errors with @Enumerated(EnumType.STRING)
-- Solution: Convert to VARCHAR(50) with CHECK constraint for validation
--
-- Author: GYDI Development Team
-- Date: 2026-01-26
-- Issue: PostgreSQL ENUM compatibility with Hibernate JPA
-- ENUM values: 'REQUEST', 'RESERVED', 'FINISHED', 'CANCELED'
-- =========================================================

-- Step 1: Drop partial indexes that have WHERE clauses referencing status ENUM
-- These indexes were created in V40
DROP INDEX IF EXISTS referrals.idx_booking_referral_active;
DROP INDEX IF EXISTS referrals.idx_booking_property_dates_status;

-- Step 2: Drop triggers that use the status column (prevent ALTER TYPE error)
DROP TRIGGER IF EXISTS trg_booking_updated_at ON referrals.booking;

-- Step 3: Drop function if it may reference the ENUM type
DROP FUNCTION IF EXISTS referrals.update_booking_updated_at() CASCADE;

-- NOTE: chk_booking_cancellation_complete constraint was already dropped in V50
-- along with the columns it references (cancellation_reason, canceled_at)

-- Step 4: Remove default value (eliminates ENUM dependency)
ALTER TABLE referrals.booking
    ALTER COLUMN status DROP DEFAULT;

-- Step 5: Convert column from ENUM to VARCHAR
ALTER TABLE referrals.booking
    ALTER COLUMN status TYPE VARCHAR(50) USING status::TEXT;

-- Step 6: Drop the ENUM type (now safe)
DROP TYPE IF EXISTS referrals.booking_status;

-- Step 7: Add CHECK constraint for validation
ALTER TABLE referrals.booking
    ADD CONSTRAINT chk_booking_status
    CHECK (status IN ('REQUEST', 'RESERVED', 'FINISHED', 'CANCELED'));

-- NOTE: chk_booking_cancellation_complete constraint is NOT recreated
-- because it was dropped in V50 along with the columns it uses
-- (cancellation_reason, canceled_by, canceled_at)

-- Step 8: Re-add default value (as VARCHAR literal)
ALTER TABLE referrals.booking
    ALTER COLUMN status SET DEFAULT 'REQUEST';

-- Step 9: Add index for query performance (if column is frequently queried)
CREATE INDEX IF NOT EXISTS idx_booking_status_varchar
    ON referrals.booking(status)
    WHERE status IS NOT NULL;

-- Step 10: Recreate partial indexes that were dropped in Step 1
-- Now they work with VARCHAR instead of ENUM
CREATE INDEX idx_booking_referral_active
ON referrals.booking(referral_link_id, status)
WHERE status IN ('REQUEST', 'RESERVED');

CREATE INDEX idx_booking_property_dates_status
ON referrals.booking(property_id, start_date, end_date, status)
WHERE status IN ('RESERVED', 'FINISHED');

-- Step 11: Recreate function (to ensure compatibility with VARCHAR)
CREATE OR REPLACE FUNCTION referrals.update_booking_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Step 12: Recreate trigger that was dropped in Step 2
CREATE TRIGGER trg_booking_updated_at
    BEFORE UPDATE ON referrals.booking
    FOR EACH ROW
    EXECUTE FUNCTION referrals.update_booking_updated_at();

-- =========================================================
-- Rollback Plan:
-- =========================================================
-- DROP TRIGGER IF EXISTS trg_booking_updated_at ON referrals.booking;
-- DROP INDEX IF EXISTS idx_booking_status_varchar;
-- ALTER TABLE referrals.booking ALTER COLUMN status DROP DEFAULT;
-- ALTER TABLE referrals.booking DROP CONSTRAINT IF EXISTS chk_booking_status;
-- CREATE TYPE referrals.booking_status AS ENUM ('REQUEST', 'RESERVED', 'FINISHED', 'CANCELED');
-- ALTER TABLE referrals.booking
--     ALTER COLUMN status TYPE referrals.booking_status USING status::referrals.booking_status;
-- ALTER TABLE referrals.booking ALTER COLUMN status SET DEFAULT 'REQUEST'::referrals.booking_status;
-- (Recreate trigger with ENUM)
-- =========================================================

COMMENT ON COLUMN referrals.booking.status IS 'Booking status lifecycle: REQUEST → RESERVED → FINISHED or CANCELED (converted from ENUM to VARCHAR for Hibernate compatibility)';
