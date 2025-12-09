-- V50__remove_redundant_fields_from_booking.sql
-- Purpose: Remove redundant fields from referrals.booking table
-- Author: Database Architect AI
-- Date: 2025-12-05
-- Reason: Financial data (total_amount, currency) and cancellation details belong in separate tables/systems

BEGIN;

-- =====================================================================
-- DROP CONSTRAINTS RELATED TO FIELDS WE'RE REMOVING
-- =====================================================================

ALTER TABLE referrals.booking
DROP CONSTRAINT IF EXISTS chk_booking_total_amount CASCADE;

ALTER TABLE referrals.booking
DROP CONSTRAINT IF EXISTS chk_booking_currency CASCADE;

ALTER TABLE referrals.booking
DROP CONSTRAINT IF EXISTS chk_booking_canceled_by CASCADE;

ALTER TABLE referrals.booking
DROP CONSTRAINT IF EXISTS chk_booking_cancellation_complete CASCADE;

-- =====================================================================
-- DROP THE REDUNDANT COLUMNS
-- =====================================================================

-- Drop financial columns (should be in payment/commission tables)
ALTER TABLE referrals.booking
DROP COLUMN IF EXISTS total_amount CASCADE;

ALTER TABLE referrals.booking
DROP COLUMN IF EXISTS currency CASCADE;

-- Drop cancellation details (can be tracked in audit/history tables if needed)
ALTER TABLE referrals.booking
DROP COLUMN IF EXISTS cancellation_reason CASCADE;

ALTER TABLE referrals.booking
DROP COLUMN IF EXISTS canceled_by CASCADE;

ALTER TABLE referrals.booking
DROP COLUMN IF EXISTS canceled_at CASCADE;

-- =====================================================================
-- UPDATE TABLE COMMENT
-- =====================================================================

COMMENT ON TABLE referrals.booking IS
'Stores booking requests and their lifecycle for properties referred through affiliate links.
Financial details tracked in commission_booking. Cancellation audit in separate systems.';

COMMIT;

-- =====================================================================
-- ROLLBACK INSTRUCTIONS (FOR REFERENCE ONLY - NOT EXECUTED)
-- =====================================================================
/*
To rollback this migration (manually):

ALTER TABLE referrals.booking
ADD COLUMN total_amount DECIMAL(10, 2);

ALTER TABLE referrals.booking
ADD COLUMN currency VARCHAR(3) DEFAULT 'USD';

ALTER TABLE referrals.booking
ADD COLUMN cancellation_reason TEXT;

ALTER TABLE referrals.booking
ADD COLUMN canceled_by VARCHAR(50);

ALTER TABLE referrals.booking
ADD COLUMN canceled_at TIMESTAMP WITH TIME ZONE;

-- Recreate constraints
ALTER TABLE referrals.booking
ADD CONSTRAINT chk_booking_total_amount CHECK (total_amount > 0);

ALTER TABLE referrals.booking
ADD CONSTRAINT chk_booking_currency CHECK (currency IN ('USD', 'EUR', 'GBP', 'MXN', 'COP'));

ALTER TABLE referrals.booking
ADD CONSTRAINT chk_booking_canceled_by
CHECK (canceled_by IS NULL OR canceled_by IN ('CLIENT', 'OWNER', 'ADMIN', 'SYSTEM'));

ALTER TABLE referrals.booking
ADD CONSTRAINT chk_booking_cancellation_complete
CHECK (
    (status != 'CANCELED')
    OR (status = 'CANCELED' AND cancellation_reason IS NOT NULL AND canceled_at IS NOT NULL)
);
*/

-- =====================================================================
-- POST-MIGRATION NOTES
-- =====================================================================
/*
After this migration:

1. Financial data:
   - Booking amounts are calculated from commission_booking.commission_amount
   - Or stored in a separate payment/invoice table if needed

2. Cancellation tracking:
   - Status field still indicates CANCELED state
   - Detailed cancellation info can be tracked in audit logs
   - Or create a separate booking_cancellations table if needed

3. Java code updates required:
   - Update Booking domain model (remove fields)
   - Update BookingEntity JPA entity
   - Update all DTOs that reference these fields
   - Update use cases (CreateBooking, UpdateStatus, etc.)
   - Update controllers and responses
*/
