-- V44__add_missing_payment_booking_fields.sql
-- Purpose: Add missing fields to payment.payment_booking table to match domain model
-- Author: System Architect
-- Date: 2025-11-24

BEGIN;

-- Add referral_link_id column
-- This is essential for commission tracking and should have been in original schema
ALTER TABLE payment.payment_booking
    ADD COLUMN IF NOT EXISTS referral_link_id BIGINT;

-- Add foreign key constraint for referral_link_id
-- Note: Using DO block to make it idempotent
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'fk_payment_booking_referral_link'
    ) THEN
        ALTER TABLE payment.payment_booking
            ADD CONSTRAINT fk_payment_booking_referral_link
            FOREIGN KEY (referral_link_id)
            REFERENCES referrals.referral_links(id)
            ON DELETE RESTRICT
            ON UPDATE CASCADE;
    END IF;
END $$;

-- Add total_amount column
-- The total booking amount before commission calculation
ALTER TABLE payment.payment_booking
    ADD COLUMN IF NOT EXISTS total_amount DECIMAL(10, 2);

-- Add failure_reason column
-- Human-readable failure reason (separate from gateway_response JSON)
ALTER TABLE payment.payment_booking
    ADD COLUMN IF NOT EXISTS failure_reason TEXT;

-- Create index on referral_link_id for query performance
CREATE INDEX IF NOT EXISTS idx_payment_booking_referral_link_id
    ON payment.payment_booking(referral_link_id);

-- Update existing records to populate referral_link_id from referrals.booking
-- This ensures data consistency for any existing payment records
UPDATE payment.payment_booking pb
SET referral_link_id = rb.referral_link_id
FROM referrals.booking rb
WHERE pb.booking_id = rb.booking_id
AND pb.referral_link_id IS NULL;

-- Update existing records to populate total_amount from referrals.booking
UPDATE payment.payment_booking pb
SET total_amount = rb.total_amount
FROM referrals.booking rb
WHERE pb.booking_id = rb.booking_id
AND pb.total_amount IS NULL;

-- Now make referral_link_id and total_amount NOT NULL for future inserts
-- (after backfilling existing data)
ALTER TABLE payment.payment_booking
    ALTER COLUMN referral_link_id SET NOT NULL;

ALTER TABLE payment.payment_booking
    ALTER COLUMN total_amount SET NOT NULL;

-- Add check constraint for total_amount
ALTER TABLE payment.payment_booking
    ADD CONSTRAINT chk_payment_total_amount
    CHECK (total_amount >= 0);

-- Add comment
COMMENT ON COLUMN payment.payment_booking.referral_link_id IS
    'Foreign key to referral link that generated this booking and commission';

COMMENT ON COLUMN payment.payment_booking.total_amount IS
    'Total booking amount (before commission). Commission = total_amount * (percentage_commission / 100)';

COMMENT ON COLUMN payment.payment_booking.failure_reason IS
    'Human-readable reason for payment failure (optional, supplements gateway_response)';

COMMIT;

-- ============================================================================
-- ROLLBACK REFERENCE
-- ============================================================================
-- To rollback this migration (manually):
-- ALTER TABLE payment.payment_booking DROP CONSTRAINT IF EXISTS fk_payment_booking_referral_link;
-- DROP INDEX IF EXISTS idx_payment_booking_referral_link_id;
-- ALTER TABLE payment.payment_booking DROP COLUMN IF EXISTS referral_link_id;
-- ALTER TABLE payment.payment_booking DROP COLUMN IF EXISTS total_amount;
-- ALTER TABLE payment.payment_booking DROP COLUMN IF EXISTS failure_reason;
