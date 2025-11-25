-- V1.6__create_bookings_table.sql
-- Purpose: Create bookings table for tracking rental booking lifecycle
-- Author: Database Architect
-- Date: 2025-11-24

BEGIN;

-- ============================================================================
-- BOOKINGS TABLE
-- ============================================================================
CREATE TABLE IF NOT EXISTS referrals.booking (
    -- Primary Key
    booking_id BIGSERIAL PRIMARY KEY,

    -- Foreign Keys
    referral_link_id BIGINT NOT NULL,
    property_id BIGINT NOT NULL,

    -- Booking Dates
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,

    -- Client Information
    client_email VARCHAR(255) NOT NULL,
    client_first_name VARCHAR(100) NOT NULL,
    client_last_name VARCHAR(100) NOT NULL,
    client_phone VARCHAR(20),

    -- Financial Information
    total_amount DECIMAL(10, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'USD',

    -- Status
    status referrals.booking_status NOT NULL DEFAULT 'REQUEST',

    -- Cancellation Details (optional, filled when status = CANCELED)
    cancellation_reason TEXT,
    canceled_by VARCHAR(50), -- 'CLIENT', 'OWNER', 'ADMIN', 'SYSTEM'
    canceled_at TIMESTAMP WITH TIME ZONE,

    -- Audit Fields
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Constraints
    CONSTRAINT fk_booking_referral_link
        FOREIGN KEY (referral_link_id)
        REFERENCES referrals.referral_links(id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,

    CONSTRAINT fk_booking_property
        FOREIGN KEY (property_id)
        REFERENCES properties.properties(id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,

    -- Business Rules
    CONSTRAINT chk_booking_dates
        CHECK (end_date > start_date),

    CONSTRAINT chk_booking_start_date_future
        CHECK (start_date >= CURRENT_DATE),

    CONSTRAINT chk_booking_total_amount
        CHECK (total_amount > 0),

    CONSTRAINT chk_booking_currency
        CHECK (currency IN ('USD', 'EUR', 'GBP', 'MXN', 'COP')),

    CONSTRAINT chk_booking_canceled_by
        CHECK (
            canceled_by IS NULL
            OR canceled_by IN ('CLIENT', 'OWNER', 'ADMIN', 'SYSTEM')
        ),

    -- If canceled, must have reason and timestamp
    CONSTRAINT chk_booking_cancellation_complete
        CHECK (
            (status != 'CANCELED')
            OR (status = 'CANCELED' AND cancellation_reason IS NOT NULL AND canceled_at IS NOT NULL)
        )
);

-- ============================================================================
-- INDEXES
-- ============================================================================

-- Index on foreign keys for JOIN performance
CREATE INDEX idx_booking_referral_link_id
    ON referrals.booking(referral_link_id);

CREATE INDEX idx_booking_property_id
    ON referrals.booking(property_id);

-- Index on status for filtering
CREATE INDEX idx_booking_status
    ON referrals.booking(status);

-- Composite index for date range queries (find bookings in a period)
CREATE INDEX idx_booking_date_range
    ON referrals.booking(start_date, end_date);

-- Index for finding recent bookings
CREATE INDEX idx_booking_created_at
    ON referrals.booking(created_at DESC);

-- Composite index for finding active bookings by referral link
CREATE INDEX idx_booking_referral_active
    ON referrals.booking(referral_link_id, status)
    WHERE status IN ('REQUEST', 'RESERVED');

-- Composite index for property availability checks
CREATE INDEX idx_booking_property_dates_status
    ON referrals.booking(property_id, start_date, end_date, status)
    WHERE status IN ('RESERVED', 'FINISHED');

-- ============================================================================
-- TRIGGER FOR UPDATED_AT
-- ============================================================================
CREATE OR REPLACE FUNCTION referrals.update_booking_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_booking_updated_at
    BEFORE UPDATE ON referrals.booking
    FOR EACH ROW
    EXECUTE FUNCTION referrals.update_booking_updated_at();

-- ============================================================================
-- COMMENTS
-- ============================================================================
COMMENT ON TABLE referrals.booking IS
    'Stores booking requests and their lifecycle for properties referred through affiliate links';

COMMENT ON COLUMN referrals.booking.booking_id IS
    'Primary key, auto-incrementing booking identifier';

COMMENT ON COLUMN referrals.booking.referral_link_id IS
    'Foreign key to the referral link used to generate this booking';

COMMENT ON COLUMN referrals.booking.property_id IS
    'Foreign key to the property being booked (denormalized for query performance)';

COMMENT ON COLUMN referrals.booking.total_amount IS
    'Total booking amount in the specified currency (used to calculate commission)';

COMMENT ON COLUMN referrals.booking.status IS
    'Current status: REQUEST → RESERVED → FINISHED or CANCELED';

COMMENT ON COLUMN referrals.booking.cancellation_reason IS
    'Reason for cancellation (required when status = CANCELED)';

COMMENT ON COLUMN referrals.booking.canceled_by IS
    'Who initiated the cancellation: CLIENT, OWNER, ADMIN, SYSTEM';

COMMIT;

-- ============================================================================
-- ROLLBACK REFERENCE
-- ============================================================================
-- To rollback this migration (manually):
-- DROP TRIGGER IF EXISTS trg_booking_updated_at ON referrals.booking CASCADE;
-- DROP FUNCTION IF EXISTS referrals.update_booking_updated_at() CASCADE;
-- DROP TABLE IF EXISTS referrals.booking CASCADE;
