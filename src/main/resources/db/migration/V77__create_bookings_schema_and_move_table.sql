-- V77__create_bookings_schema_and_move_table.sql
-- Purpose: Create bookings bounded context schema and migrate booking table from referrals
-- Author: Database Architect AI
-- Date: 2026-02-06
-- Business Context: Separate bookings from referrals as independent bounded context

BEGIN;

-- ============================================================================
-- 1. CREATE BOOKINGS SCHEMA
-- ============================================================================

CREATE SCHEMA IF NOT EXISTS bookings;

COMMENT ON SCHEMA bookings IS
'Bounded context for booking lifecycle management. Handles guest reservations from REQUEST to FINISHED.';


-- ============================================================================
-- 2. CREATE BOOKING TABLE WITH ENHANCED FIELDS
-- ============================================================================

CREATE TABLE bookings.booking (
    id BIGSERIAL PRIMARY KEY,

    -- Relationships
    referral_link_id BIGINT NOT NULL,
    property_id BIGINT NOT NULL,

    -- Booking Dates
    check_in_date DATE NOT NULL,
    check_out_date DATE NOT NULL,

    -- Guest Information
    guest_name VARCHAR(255) NOT NULL,
    guest_email VARCHAR(255) NOT NULL,
    guest_phone VARCHAR(50),
    guests_count INT NOT NULL DEFAULT 1,

    -- Airbnb Integration
    airbnb_confirmation_code VARCHAR(100),

    -- Financial
    total_amount NUMERIC(10,2),  -- NULL in REQUEST, required when RESERVED
    currency VARCHAR(3) NOT NULL DEFAULT 'USD',

    -- Booking Status
    status VARCHAR(50) NOT NULL DEFAULT 'REQUEST',

    -- Lifecycle Timestamps (track when each status was reached)
    reserved_at TIMESTAMP WITH TIME ZONE,    -- When ADMIN confirms and sets RESERVED
    started_at TIMESTAMP WITH TIME ZONE,     -- When check-in occurs (IN_PROGRESS)
    finished_at TIMESTAMP WITH TIME ZONE,    -- When check-out occurs (FINISHED)
    cancelled_at TIMESTAMP WITH TIME ZONE,   -- If cancelled at any point

    -- Audit Timestamps
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Foreign Key Constraints
    CONSTRAINT fk_booking_referral_link
        FOREIGN KEY (referral_link_id)
        REFERENCES referrals.referral_links(id)
        ON DELETE RESTRICT,  -- Don't allow deleting referral link if bookings exist

    CONSTRAINT fk_booking_property
        FOREIGN KEY (property_id)
        REFERENCES properties.properties(id)
        ON DELETE RESTRICT,  -- Don't allow deleting property if bookings exist

    -- Business Rule Constraints
    CONSTRAINT chk_booking_dates
        CHECK (check_out_date > check_in_date),

    CONSTRAINT chk_booking_guests
        CHECK (guests_count > 0 AND guests_count <= 50),

    CONSTRAINT chk_booking_amount
        CHECK (total_amount IS NULL OR total_amount > 0),

    CONSTRAINT chk_booking_status
        CHECK (status IN ('REQUEST', 'RESERVED', 'IN_PROGRESS', 'FINISHED', 'CANCELLED', 'DISPUTED')),

    CONSTRAINT chk_booking_currency
        CHECK (currency IN ('USD', 'EUR', 'GBP', 'MXN'))
);


-- ============================================================================
-- 3. MIGRATE DATA FROM REFERRALS.BOOKING
-- ============================================================================

-- Insert existing bookings into new table
INSERT INTO bookings.booking (
    id,
    referral_link_id,
    property_id,
    check_in_date,
    check_out_date,
    guest_name,
    guest_email,
    guest_phone,
    guests_count,
    airbnb_confirmation_code,
    total_amount,
    currency,
    status,
    reserved_at,
    started_at,
    finished_at,
    cancelled_at,
    created_at,
    updated_at
)
SELECT
    booking_id,
    referral_link_id,
    property_id,
    start_date,
    end_date,
    CONCAT(TRIM(client_first_name), ' ', TRIM(client_last_name)),  -- Combine names
    client_email,
    client_phone,
    1,  -- Default guests_count for existing bookings
    NULL,  -- airbnb_confirmation_code (new field, no existing data)
    NULL,  -- total_amount (new field, no existing data)
    'USD',  -- Default currency

    -- Map old status to new (assuming old status was uppercase)
    UPPER(status),

    -- Estimate lifecycle timestamps based on status and existing dates
    CASE
        WHEN UPPER(status) IN ('RESERVED', 'IN_PROGRESS', 'FINISHED')
        THEN created_at
        ELSE NULL
    END,  -- reserved_at

    CASE
        WHEN UPPER(status) IN ('IN_PROGRESS', 'FINISHED')
        THEN start_date::timestamp with time zone
        ELSE NULL
    END,  -- started_at

    CASE
        WHEN UPPER(status) = 'FINISHED'
        THEN end_date::timestamp with time zone
        ELSE NULL
    END,  -- finished_at

    CASE
        WHEN UPPER(status) IN ('CANCELLED', 'CANCELED')  -- Handle both spellings
        THEN COALESCE(updated_at, created_at)
        ELSE NULL
    END,  -- cancelled_at

    created_at,
    COALESCE(updated_at, created_at)
FROM referrals.booking;


-- ============================================================================
-- 4. UPDATE FOREIGN KEY IN COMMISSION_BOOKING
-- ============================================================================

-- Drop old constraint
ALTER TABLE referrals.commission_booking
DROP CONSTRAINT IF EXISTS referrals_commission_booking_booking_id_fkey;

ALTER TABLE referrals.commission_booking
DROP CONSTRAINT IF EXISTS fk_commission_booking_booking;

-- Add new constraint pointing to bookings.booking
ALTER TABLE referrals.commission_booking
ADD CONSTRAINT fk_commission_booking_booking
    FOREIGN KEY (booking_id)
    REFERENCES bookings.booking(id)
    ON DELETE CASCADE;  -- If booking deleted, cascade delete commission records

COMMENT ON CONSTRAINT fk_commission_booking_booking ON referrals.commission_booking IS
'Temporary constraint. This table will be deprecated in V79 when commissions schema is created.';


-- ============================================================================
-- 5. CREATE INDEXES FOR PERFORMANCE
-- ============================================================================

-- Foreign key indexes (critical for JOIN performance)
CREATE INDEX idx_booking_referral_link
    ON bookings.booking(referral_link_id);

CREATE INDEX idx_booking_property
    ON bookings.booking(property_id);

-- Query optimization indexes
CREATE INDEX idx_booking_status
    ON bookings.booking(status);

CREATE INDEX idx_booking_dates
    ON bookings.booking(check_in_date, check_out_date);

CREATE INDEX idx_booking_created_at
    ON bookings.booking(created_at DESC);

-- Scheduler optimization - find bookings ready for status transitions
CREATE INDEX idx_booking_check_in_pending
    ON bookings.booking(check_in_date, status)
    WHERE status = 'RESERVED';

CREATE INDEX idx_booking_check_out_active
    ON bookings.booking(check_out_date, status)
    WHERE status = 'IN_PROGRESS';

-- Guest lookup (for support/admin)
CREATE INDEX idx_booking_guest_email
    ON bookings.booking(guest_email);


-- ============================================================================
-- 6. CREATE UPDATED_AT TRIGGER
-- ============================================================================

CREATE OR REPLACE FUNCTION bookings.update_booking_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER tr_booking_updated_at
    BEFORE UPDATE ON bookings.booking
    FOR EACH ROW
    EXECUTE FUNCTION bookings.update_booking_updated_at();


-- ============================================================================
-- 7. RESET SEQUENCE TO CONTINUE FROM MAX ID
-- ============================================================================

-- Ensure next booking ID continues from where we left off
SELECT setval(
    'bookings.booking_id_seq',
    COALESCE((SELECT MAX(id) FROM bookings.booking), 1),
    true  -- is_called = true means next nextval() will increment
);


-- ============================================================================
-- 8. ADD TABLE AND COLUMN COMMENTS (DOCUMENTATION)
-- ============================================================================

COMMENT ON TABLE bookings.booking IS
'Booking lifecycle management. Status flow: REQUEST → RESERVED → IN_PROGRESS → FINISHED (or CANCELLED/DISPUTED at any point). Commissions calculated when status = FINISHED.';

COMMENT ON COLUMN bookings.booking.id IS
'Primary key. Unique booking identifier.';

COMMENT ON COLUMN bookings.booking.referral_link_id IS
'Reference to the affiliate referral link that generated this booking. Used for commission calculation.';

COMMENT ON COLUMN bookings.booking.property_id IS
'Reference to the property being booked.';

COMMENT ON COLUMN bookings.booking.check_in_date IS
'Guest check-in date. Scheduler triggers status change to IN_PROGRESS on this date.';

COMMENT ON COLUMN bookings.booking.check_out_date IS
'Guest check-out date. Scheduler triggers status change to FINISHED on this date.';

COMMENT ON COLUMN bookings.booking.guest_name IS
'Full name of the guest making the reservation.';

COMMENT ON COLUMN bookings.booking.guests_count IS
'Number of guests for the reservation. Must be > 0.';

COMMENT ON COLUMN bookings.booking.airbnb_confirmation_code IS
'Airbnb confirmation code entered by co-host/ADMIN when marking booking as RESERVED. Required for status transition REQUEST → RESERVED.';

COMMENT ON COLUMN bookings.booking.total_amount IS
'Total booking amount used for commission calculation. NULL in REQUEST status, required when marking as RESERVED or later.';

COMMENT ON COLUMN bookings.booking.status IS
'Current booking status. Valid values: REQUEST (initial), RESERVED (confirmed by admin), IN_PROGRESS (guest checked in), FINISHED (guest checked out), CANCELLED (cancelled by admin/host/guest), DISPUTED (dispute after completion).';

COMMENT ON COLUMN bookings.booking.reserved_at IS
'Timestamp when booking was confirmed (status changed to RESERVED). Set by ADMIN when entering Airbnb confirmation code.';

COMMENT ON COLUMN bookings.booking.started_at IS
'Timestamp when guest checked in (status changed to IN_PROGRESS). Typically set automatically by scheduler on check_in_date.';

COMMENT ON COLUMN bookings.booking.finished_at IS
'Timestamp when guest checked out (status changed to FINISHED). Typically set automatically by scheduler on check_out_date. Triggers commission calculation.';

COMMENT ON COLUMN bookings.booking.cancelled_at IS
'Timestamp when booking was cancelled (status changed to CANCELLED). Can occur at any point in lifecycle.';


-- ============================================================================
-- 9. VALIDATION QUERY (RUN AFTER MIGRATION)
-- ============================================================================

-- Verify data migration was successful
DO $$
DECLARE
    old_count BIGINT;
    new_count BIGINT;
BEGIN
    SELECT COUNT(*) INTO old_count FROM referrals.booking;
    SELECT COUNT(*) INTO new_count FROM bookings.booking;

    IF old_count != new_count THEN
        RAISE EXCEPTION 'Data migration failed: referrals.booking has % rows but bookings.booking has % rows',
            old_count, new_count;
    END IF;

    RAISE NOTICE 'Migration successful: % bookings migrated from referrals.booking to bookings.booking', new_count;
END $$;


-- ============================================================================
-- 10. PRESERVE OLD TABLE (SAFETY - DROP LATER AFTER VALIDATION)
-- ============================================================================

-- ⚠️ DO NOT DROP OLD TABLE YET - Keep for 1-2 weeks for validation
-- Once validated in production, uncomment and apply in separate migration:
-- DROP TABLE IF EXISTS referrals.booking CASCADE;

COMMENT ON TABLE referrals.booking IS
'⚠️ DEPRECATED: Migrated to bookings.booking in V77. This table will be dropped in a future migration after validation period. DO NOT USE.';


COMMIT;

-- ============================================================================
-- ROLLBACK INSTRUCTIONS (IF NEEDED)
-- ============================================================================

-- To rollback this migration:
--
-- BEGIN;
--
-- -- 1. Restore FK in commission_booking
-- ALTER TABLE referrals.commission_booking
-- DROP CONSTRAINT fk_commission_booking_booking;
--
-- ALTER TABLE referrals.commission_booking
-- ADD CONSTRAINT referrals_commission_booking_booking_id_fkey
--     FOREIGN KEY (booking_id)
--     REFERENCES referrals.booking(booking_id)
--     ON DELETE CASCADE;
--
-- -- 2. Drop bookings schema
-- DROP SCHEMA IF EXISTS bookings CASCADE;
--
-- -- 3. Remove deprecation comment
-- COMMENT ON TABLE referrals.booking IS 'Booking records generated from referral links.';
--
-- COMMIT;
