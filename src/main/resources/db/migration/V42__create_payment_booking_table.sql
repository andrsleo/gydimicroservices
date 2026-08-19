-- V1.8__create_payment_booking_table.sql
-- Purpose: Create payment_booking table for commission payments to affiliates
-- Author: Database Architect
-- Date: 2025-11-24

BEGIN;

-- ============================================================================
-- PAYMENT BOOKING TABLE
-- ============================================================================
-- This table is created ONLY when referrals.booking.status = 'FINISHED'
-- It represents the commission payment owed to the referring user

CREATE TABLE IF NOT EXISTS payment.payment_booking (
    -- Primary Key
    id BIGSERIAL PRIMARY KEY,

    -- Foreign Keys
    booking_id BIGINT NOT NULL UNIQUE, -- 1:1 relationship with referrals.booking
    user_id BIGINT NOT NULL,           -- The referring user (earns commission)
    property_id BIGINT NOT NULL,       -- Denormalized for query performance

    -- Commission Calculation
    percentage_commission DECIMAL(5, 2) NOT NULL, -- e.g., 5.00 for 5%
    commission_amount DECIMAL(10, 2) NOT NULL,    -- Actual amount to pay
    currency VARCHAR(3) NOT NULL DEFAULT 'USD',

    -- Payment Status
    payment_status payment.payment_status NOT NULL DEFAULT 'PENDING',

    -- Payment Gateway Details (extensibility for future integrations)
    payment_method VARCHAR(50),        -- 'STRIPE', 'PAYPAL', 'BANK_TRANSFER', 'MANUAL'
    transaction_id VARCHAR(255),       -- Gateway transaction ID
    gateway_name VARCHAR(100),         -- Gateway provider name
    gateway_response JSONB,            -- Full gateway response for debugging

    -- Payment Execution
    paid_at TIMESTAMP WITH TIME ZONE,  -- When payment was successfully completed

    -- Audit Fields
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Constraints
    CONSTRAINT fk_payment_booking_booking
        FOREIGN KEY (booking_id)
        REFERENCES referrals.booking(booking_id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,

    CONSTRAINT fk_payment_booking_user
        FOREIGN KEY (user_id)
        REFERENCES users.users(id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,

    CONSTRAINT fk_payment_booking_property
        FOREIGN KEY (property_id)
        REFERENCES properties.properties(id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,

    -- Business Rules
    CONSTRAINT chk_payment_percentage_commission
        CHECK (percentage_commission >= 0 AND percentage_commission <= 100),

    CONSTRAINT chk_payment_commission_amount
        CHECK (commission_amount >= 0),

    CONSTRAINT chk_payment_currency
        CHECK (currency IN ('USD', 'EUR', 'GBP', 'MXN', 'COP')),

    CONSTRAINT chk_payment_method
        CHECK (
            payment_method IS NULL
            OR payment_method IN ('STRIPE', 'PAYPAL', 'BANK_TRANSFER', 'MANUAL', 'OTHER')
        ),

    -- If payment is SUCCESS, must have paid_at timestamp
    CONSTRAINT chk_payment_success_paid_at
        CHECK (
            (payment_status != 'SUCCESS')
            OR (payment_status = 'SUCCESS' AND paid_at IS NOT NULL)
        ),

    -- If payment is SUCCESS, should have transaction_id
    CONSTRAINT chk_payment_success_transaction_id
        CHECK (
            (payment_status != 'SUCCESS')
            OR (payment_status = 'SUCCESS' AND transaction_id IS NOT NULL)
        )
);

-- ============================================================================
-- INDEXES
-- ============================================================================

-- Index on foreign keys for JOIN performance
CREATE INDEX idx_payment_booking_booking_id
    ON payment.payment_booking(booking_id);

CREATE INDEX idx_payment_booking_user_id
    ON payment.payment_booking(user_id);

CREATE INDEX idx_payment_booking_property_id
    ON payment.payment_booking(property_id);

-- Index on payment status for filtering
CREATE INDEX idx_payment_booking_status
    ON payment.payment_booking(payment_status);

-- Index for finding recent payments
CREATE INDEX idx_payment_booking_created_at
    ON payment.payment_booking(created_at DESC);

-- Index for finding successful payments by user (earnings history)
CREATE INDEX idx_payment_booking_user_success
    ON payment.payment_booking(user_id, payment_status, paid_at DESC)
    WHERE payment_status = 'SUCCESS';

-- Index for finding pending payments (for batch processing)
CREATE INDEX idx_payment_booking_pending
    ON payment.payment_booking(payment_status, created_at)
    WHERE payment_status IN ('PENDING', 'PROCESSING');

-- Index on transaction_id for idempotency checks
CREATE INDEX idx_payment_booking_transaction_id
    ON payment.payment_booking(transaction_id)
    WHERE transaction_id IS NOT NULL;

-- ============================================================================
-- TRIGGER FOR UPDATED_AT
-- ============================================================================
CREATE OR REPLACE FUNCTION payment.update_payment_booking_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_payment_booking_updated_at
    BEFORE UPDATE ON payment.payment_booking
    FOR EACH ROW
    EXECUTE FUNCTION payment.update_payment_booking_updated_at();

-- ============================================================================
-- TRIGGER FOR paid_at TIMESTAMP
-- ============================================================================
-- Automatically set paid_at when payment_status changes to SUCCESS
CREATE OR REPLACE FUNCTION payment.set_payment_booking_paid_at()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.payment_status = 'SUCCESS' AND OLD.payment_status != 'SUCCESS' THEN
        NEW.paid_at = CURRENT_TIMESTAMP;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_payment_booking_paid_at
    BEFORE UPDATE ON payment.payment_booking
    FOR EACH ROW
    EXECUTE FUNCTION payment.set_payment_booking_paid_at();

-- ============================================================================
-- COMMENTS
-- ============================================================================
COMMENT ON TABLE payment.payment_booking IS
    'Commission payments owed to referring users when bookings are completed';

COMMENT ON COLUMN payment.payment_booking.id IS
    'Primary key, auto-incrementing payment identifier';

COMMENT ON COLUMN payment.payment_booking.booking_id IS
    'Foreign key to referrals.booking (UNIQUE - one payment per booking)';

COMMENT ON COLUMN payment.payment_booking.user_id IS
    'Foreign key to the referring user who earns this commission';

COMMENT ON COLUMN payment.payment_booking.percentage_commission IS
    'Commission rate from user subscription plan at time of booking completion';

COMMENT ON COLUMN payment.payment_booking.commission_amount IS
    'Calculated commission: booking.total_amount * (percentage_commission / 100)';

COMMENT ON COLUMN payment.payment_booking.payment_status IS
    'Current payment status: PENDING → PROCESSING → SUCCESS or FAILED';

COMMENT ON COLUMN payment.payment_booking.gateway_response IS
    'Full JSON response from payment gateway for debugging and reconciliation';

COMMENT ON COLUMN payment.payment_booking.paid_at IS
    'Timestamp when payment was successfully completed (auto-set on SUCCESS)';

COMMIT;

-- ============================================================================
-- ROLLBACK REFERENCE
-- ============================================================================
-- To rollback this migration (manually):
-- DROP TRIGGER IF EXISTS trg_payment_booking_paid_at ON payment.payment_booking CASCADE;
-- DROP TRIGGER IF EXISTS trg_payment_booking_updated_at ON payment.payment_booking CASCADE;
-- DROP FUNCTION IF EXISTS payment.set_payment_booking_paid_at() CASCADE;
-- DROP FUNCTION IF EXISTS payment.update_payment_booking_updated_at() CASCADE;
-- DROP TABLE IF EXISTS payment.payment_booking CASCADE;
