-- V80__cleanup_legacy_tables_and_fix_pii_columns.sql
-- Purpose:
--   1. Fix PII-encrypted columns in bookings.booking to use TEXT (encrypted values exceed VARCHAR limits)
--   2. Drop deprecated referrals.booking table (data already migrated to bookings.booking in V77)
--   3. Drop deprecated referrals.commission_booking table (replaced by commissions schema in V79)
--   4. Clean up orphaned triggers, functions, and enum types
-- Author: Database Architect AI
-- Date: 2026-02-11
-- Prerequisites: V77 (booking migration), V78 (status history), V79 (commissions schema)

BEGIN;

-- ============================================================================
-- 1. FIX PII-ENCRYPTED COLUMNS IN bookings.booking
-- ============================================================================
-- The PiiEncryptor converts plain text to encrypted strings that are much longer
-- than the original text. A 20-char email encrypted becomes ~200+ chars.
-- Columns guest_name, guest_email were VARCHAR(255) and guest_phone was VARCHAR(50).
-- All need to be TEXT to safely store encrypted values.

ALTER TABLE bookings.booking
    ALTER COLUMN guest_name TYPE TEXT;

ALTER TABLE bookings.booking
    ALTER COLUMN guest_email TYPE TEXT;

ALTER TABLE bookings.booking
    ALTER COLUMN guest_phone TYPE TEXT;

COMMENT ON COLUMN bookings.booking.guest_name IS
'Full name of the guest (PII encrypted at rest via PiiEncryptor). Stored as TEXT to accommodate encrypted value length.';

COMMENT ON COLUMN bookings.booking.guest_email IS
'Email of the guest (PII encrypted at rest via PiiEncryptor). Stored as TEXT to accommodate encrypted value length.';

COMMENT ON COLUMN bookings.booking.guest_phone IS
'Phone of the guest (PII encrypted at rest via PiiEncryptor). Stored as TEXT to accommodate encrypted value length.';


-- ============================================================================
-- 2. DROP referrals.commission_booking AND ITS DEPENDENCIES
-- ============================================================================
-- This table was deprecated in V79 when the commissions schema was created.
-- The new commissions bounded context uses commissions.affiliate_commission
-- and commissions.host_commission tables instead.

-- 2a. Drop triggers on referrals.commission_booking
DROP TRIGGER IF EXISTS tr_enforce_ledger_immutability ON referrals.commission_booking;
DROP TRIGGER IF EXISTS tr_audit_commission_status_change ON referrals.commission_booking;
DROP TRIGGER IF EXISTS tr_commission_booking_updated_at ON referrals.commission_booking;
DROP TRIGGER IF EXISTS tr_calculate_verification_hash ON referrals.commission_booking;

-- 2b. Drop the table (CASCADE drops its indexes and constraints)
DROP TABLE IF EXISTS referrals.commission_booking CASCADE;

-- 2c. Drop orphaned functions that were specific to commission_booking
DROP FUNCTION IF EXISTS referrals.enforce_ledger_immutability() CASCADE;
DROP FUNCTION IF EXISTS referrals.audit_commission_status_change() CASCADE;
DROP FUNCTION IF EXISTS referrals.calculate_verification_hash() CASCADE;

-- 2d. Drop the commission_status enum type if it exists (was converted to VARCHAR in V68)
DROP TYPE IF EXISTS referrals.commission_status CASCADE;


-- ============================================================================
-- 3. DROP referrals.booking AND ITS DEPENDENCIES
-- ============================================================================
-- This table was deprecated in V77 when data was migrated to bookings.booking.
-- The deprecation comment was added in V77, and VALIDATE_V77_V78_V79.sql
-- recommended creating this cleanup migration.

-- 3a. Drop triggers on referrals.booking
DROP TRIGGER IF EXISTS trg_booking_updated_at ON referrals.booking;
DROP TRIGGER IF EXISTS tr_booking_updated_at ON referrals.booking;

-- 3b. Drop the table (CASCADE drops its indexes and constraints)
DROP TABLE IF EXISTS referrals.booking CASCADE;

-- 3c. Drop orphaned functions specific to referrals.booking
DROP FUNCTION IF EXISTS referrals.update_booking_updated_at() CASCADE;

-- 3d. Drop the booking_status enum type if it exists (was created in V39, used by old referrals.booking)
DROP TYPE IF EXISTS referrals.booking_status CASCADE;


-- ============================================================================
-- 4. VERIFICATION
-- ============================================================================

DO $$
DECLARE
    booking_exists BOOLEAN;
    commission_exists BOOLEAN;
    col_type TEXT;
BEGIN
    -- Verify referrals.booking was dropped
    SELECT EXISTS (
        SELECT 1 FROM information_schema.tables
        WHERE table_schema = 'referrals' AND table_name = 'booking'
    ) INTO booking_exists;

    IF booking_exists THEN
        RAISE EXCEPTION 'referrals.booking still exists after DROP';
    END IF;

    -- Verify referrals.commission_booking was dropped
    SELECT EXISTS (
        SELECT 1 FROM information_schema.tables
        WHERE table_schema = 'referrals' AND table_name = 'commission_booking'
    ) INTO commission_exists;

    IF commission_exists THEN
        RAISE EXCEPTION 'referrals.commission_booking still exists after DROP';
    END IF;

    -- Verify bookings.booking PII columns are TEXT
    SELECT data_type INTO col_type
    FROM information_schema.columns
    WHERE table_schema = 'bookings' AND table_name = 'booking' AND column_name = 'guest_name';

    IF col_type != 'text' THEN
        RAISE EXCEPTION 'guest_name column type is % instead of TEXT', col_type;
    END IF;

    SELECT data_type INTO col_type
    FROM information_schema.columns
    WHERE table_schema = 'bookings' AND table_name = 'booking' AND column_name = 'guest_email';

    IF col_type != 'text' THEN
        RAISE EXCEPTION 'guest_email column type is % instead of TEXT', col_type;
    END IF;

    SELECT data_type INTO col_type
    FROM information_schema.columns
    WHERE table_schema = 'bookings' AND table_name = 'booking' AND column_name = 'guest_phone';

    IF col_type != 'text' THEN
        RAISE EXCEPTION 'guest_phone column type is % instead of TEXT', col_type;
    END IF;

    RAISE NOTICE 'V80 Migration successful:';
    RAISE NOTICE '  ✓ bookings.booking PII columns changed to TEXT';
    RAISE NOTICE '  ✓ referrals.booking dropped';
    RAISE NOTICE '  ✓ referrals.commission_booking dropped';
    RAISE NOTICE '  ✓ Orphaned triggers, functions, and enum types cleaned up';
END $$;

COMMIT;
