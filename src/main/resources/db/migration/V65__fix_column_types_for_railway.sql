-- =====================================================
-- V65: Fix column types to match Railway PostgreSQL
-- =====================================================
-- Author: GYDI Development Team
-- Date: 2026-01-23
-- Description: Corrects column types that were causing validation errors in Railway
--
-- Changes:
-- 1. payment.payment_booking.gateway_response: TEXT -> JSONB
-- 2. subscriptions.payment_methods.card_last_four: CHAR -> VARCHAR(4)
--
-- Reason: Hibernate schema validation was failing because JPA entity definitions
-- didn't match the actual database column types in Railway PostgreSQL.
-- =====================================================

-- Fix payment_booking.gateway_response column type
ALTER TABLE payment.payment_booking
    ALTER COLUMN gateway_response TYPE jsonb USING gateway_response::jsonb;

COMMENT ON COLUMN payment.payment_booking.gateway_response IS 'Payment gateway response data (JSONB for structured storage)';

-- Fix payment_methods.card_last_four column type
ALTER TABLE subscriptions.payment_methods
    ALTER COLUMN card_last_four TYPE VARCHAR(4);

COMMENT ON COLUMN subscriptions.payment_methods.card_last_four IS 'Last 4 digits of the card (VARCHAR for consistency)';
