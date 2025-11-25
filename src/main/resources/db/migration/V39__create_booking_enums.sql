-- V1.5__create_booking_enums.sql
-- Purpose: Create ENUM type for booking status
-- Author: Database Architect
-- Date: 2025-11-24

-- ============================================================================
-- BOOKING STATUS ENUM
-- ============================================================================
-- Status lifecycle:
-- REQUEST → RESERVED → FINISHED → (end state)
-- REQUEST → CANCELED (can cancel at any point before FINISHED)
-- RESERVED → CANCELED

CREATE TYPE referrals.booking_status AS ENUM (
    'REQUEST',   -- Initial booking request from client
    'RESERVED',  -- Booking confirmed by property owner
    'FINISHED',  -- Booking completed (client checked out)
    'CANCELED'   -- Booking canceled (by client or owner)
);

COMMENT ON TYPE referrals.booking_status IS 'Lifecycle status of a booking';

-- ============================================================================
-- NOTE: payment.payment_status ENUM is created in V1.7 after payment schema
-- ============================================================================

-- ============================================================================
-- ROLLBACK REFERENCE
-- ============================================================================
-- To rollback this migration (manually):
-- DROP TYPE IF EXISTS referrals.booking_status CASCADE;
