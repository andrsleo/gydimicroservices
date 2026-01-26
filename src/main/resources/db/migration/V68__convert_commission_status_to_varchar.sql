-- =========================================================
-- Migration V68: Convert referrals.commission_status from ENUM to VARCHAR
-- =========================================================
-- Purpose: Fix PostgreSQL ENUM type mismatch error with Hibernate
-- Context: referrals.commission_status ENUM causes casting errors with @Enumerated(EnumType.STRING)
-- Solution: Convert to VARCHAR(50) with CHECK constraint for validation
--
-- Author: GYDI Development Team
-- Date: 2026-01-26
-- Issue: PostgreSQL ENUM compatibility with Hibernate JPA
-- ENUM values: 'PENDING', 'APPROVED', 'PAID', 'REJECTED', 'REFUNDED'
-- NOTE: Table was renamed from commission_ledger to commission_booking in V48
-- =========================================================

-- Step 1: Drop partial indexes that have WHERE clauses referencing status ENUM
-- These indexes were created in V48 (recreated from V29 and V31)
DROP INDEX IF EXISTS referrals.idx_commission_booking_approved;
DROP INDEX IF EXISTS referrals.idx_commission_booking_pending_review;
-- NOTE: idx_commission_ledger_payout_batch was NOT recreated in V48 (only existed in V31)

-- Step 2: Drop triggers that use the status column (prevent ALTER TYPE error)
DROP TRIGGER IF EXISTS tr_enforce_ledger_immutability ON referrals.commission_booking;
DROP TRIGGER IF EXISTS tr_audit_commission_status_change ON referrals.commission_booking;

-- Step 3: Drop functions that may reference the ENUM type
DROP FUNCTION IF EXISTS referrals.enforce_ledger_immutability() CASCADE;
DROP FUNCTION IF EXISTS referrals.audit_commission_status_change() CASCADE;

-- Step 4: Drop CHECK constraint that references ENUM type
-- This constraint was created in V29 and compares status with ENUM values
ALTER TABLE referrals.commission_booking
    DROP CONSTRAINT IF EXISTS valid_status_workflow;

-- Step 5: Remove default value (eliminates ENUM dependency)
ALTER TABLE referrals.commission_booking
    ALTER COLUMN status DROP DEFAULT;

-- Step 6: Convert column from ENUM to VARCHAR
ALTER TABLE referrals.commission_booking
    ALTER COLUMN status TYPE VARCHAR(50) USING status::TEXT;

-- Step 7: Drop the ENUM type (now safe)
DROP TYPE IF EXISTS referrals.commission_status;

-- Step 8: Add CHECK constraint for validation
ALTER TABLE referrals.commission_booking
    ADD CONSTRAINT chk_commission_booking_status
    CHECK (status IN ('PENDING', 'APPROVED', 'PAID', 'REJECTED', 'REFUNDED'));

-- Step 9: Recreate the workflow constraint (from V29) with VARCHAR
-- This ensures status workflow integrity: PENDING → APPROVED → PAID
ALTER TABLE referrals.commission_booking
    ADD CONSTRAINT valid_status_workflow CHECK (
        (status = 'PENDING' AND approved_at IS NULL AND paid_at IS NULL) OR
        (status = 'APPROVED' AND approved_at IS NOT NULL AND paid_at IS NULL) OR
        (status = 'PAID' AND approved_at IS NOT NULL AND paid_at IS NOT NULL) OR
        (status IN ('REJECTED', 'REFUNDED'))
    );

-- Step 10: Re-add default value (as VARCHAR literal)
ALTER TABLE referrals.commission_booking
    ALTER COLUMN status SET DEFAULT 'PENDING';

-- Step 11: Add index for query performance (if column is frequently queried)
CREATE INDEX IF NOT EXISTS idx_commission_booking_status
    ON referrals.commission_booking(status)
    WHERE status IS NOT NULL;

-- Step 12: Recreate trigger functions to work with VARCHAR instead of ENUM
CREATE OR REPLACE FUNCTION referrals.enforce_ledger_immutability()
RETURNS TRIGGER AS $$
BEGIN
    -- Allow status changes (normal workflow)
    IF OLD.status != NEW.status THEN
        -- Status change is allowed
        -- But verify other fields remain unchanged
        IF OLD.booking_id != NEW.booking_id OR
           OLD.commission_rate != NEW.commission_rate OR
           OLD.commission_amount != NEW.commission_amount OR
           OLD.affiliate_plan != NEW.affiliate_plan OR
           OLD.verification_hash != NEW.verification_hash THEN
            RAISE EXCEPTION 'Immutable ledger violation: Cannot modify financial fields (only status can change)';
        END IF;

        -- Update workflow timestamps
        IF NEW.status = 'APPROVED' AND OLD.status = 'PENDING' THEN
            NEW.approved_at = CURRENT_TIMESTAMP;
        ELSIF NEW.status = 'PAID' AND OLD.status = 'APPROVED' THEN
            NEW.paid_at = CURRENT_TIMESTAMP;
        END IF;
    ELSE
        -- No status change = no changes allowed
        RAISE EXCEPTION 'Immutable ledger violation: Cannot modify commission records after creation';
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION referrals.audit_commission_status_change()
RETURNS TRIGGER AS $$
BEGIN
    IF OLD.status IS DISTINCT FROM NEW.status THEN
        INSERT INTO referrals.security_audit_log (
            event_type,
            user_id,
            details
        ) VALUES (
            'COMMISSION_STATUS_CHANGE',
            (SELECT affiliate_id FROM referrals.booking WHERE booking_id = NEW.booking_id),
            jsonb_build_object(
                'commission_id', NEW.id,
                'old_status', OLD.status,
                'new_status', NEW.status,
                'commission_amount', NEW.commission_amount,
                'changed_at', CURRENT_TIMESTAMP,
                'changed_by', CURRENT_USER
            )
        );
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Step 13: Recreate triggers that were dropped in Step 2
CREATE TRIGGER tr_enforce_ledger_immutability
    BEFORE UPDATE ON referrals.commission_booking
    FOR EACH ROW
    EXECUTE FUNCTION referrals.enforce_ledger_immutability();

-- Note: Removed WHEN clause because it references the column type which was just converted
-- The WHEN condition is already handled inside the function itself
CREATE TRIGGER tr_audit_commission_status_change
    AFTER UPDATE ON referrals.commission_booking
    FOR EACH ROW
    EXECUTE FUNCTION referrals.audit_commission_status_change();

-- Step 14: Recreate partial indexes that were dropped in Step 1
-- Now they work with VARCHAR instead of ENUM
CREATE INDEX idx_commission_booking_approved
ON referrals.commission_booking(status, approved_at)
WHERE status = 'APPROVED';

CREATE INDEX idx_commission_booking_pending_review
ON referrals.commission_booking(status, created_at)
WHERE status = 'PENDING';

-- =========================================================
-- Rollback Plan:
-- =========================================================
-- DROP TRIGGER IF EXISTS tr_audit_commission_status_change ON referrals.commission_booking;
-- DROP TRIGGER IF EXISTS tr_enforce_ledger_immutability ON referrals.commission_booking;
-- DROP INDEX IF EXISTS idx_commission_booking_status;
-- ALTER TABLE referrals.commission_booking ALTER COLUMN status DROP DEFAULT;
-- ALTER TABLE referrals.commission_booking DROP CONSTRAINT IF EXISTS chk_commission_booking_status;
-- CREATE TYPE referrals.commission_status AS ENUM ('PENDING', 'APPROVED', 'PAID', 'REJECTED', 'REFUNDED');
-- ALTER TABLE referrals.commission_booking
--     ALTER COLUMN status TYPE referrals.commission_status USING status::referrals.commission_status;
-- ALTER TABLE referrals.commission_booking ALTER COLUMN status SET DEFAULT 'PENDING'::referrals.commission_status;
-- (Recreate triggers with ENUM)
-- =========================================================

COMMENT ON COLUMN referrals.commission_booking.status IS 'Commission status lifecycle: PENDING → APPROVED → PAID, or REJECTED/REFUNDED (converted from ENUM to VARCHAR for Hibernate compatibility)';
