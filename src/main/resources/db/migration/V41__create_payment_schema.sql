-- V1.7__create_payment_schema.sql
-- Purpose: Create payment schema and payment status ENUM
-- Author: Database Architect
-- Date: 2025-11-24

BEGIN;

-- ============================================================================
-- PAYMENT SCHEMA
-- ============================================================================
-- Separating payment logic into its own schema follows DDD bounded context
-- principles and allows for independent scaling/security policies

CREATE SCHEMA IF NOT EXISTS payment;

COMMENT ON SCHEMA payment IS
    'Payment domain - handles commission payments to referring users';

-- ============================================================================
-- PAYMENT STATUS ENUM
-- ============================================================================
-- Status lifecycle:
-- PENDING → PROCESSING → SUCCESS (end state)
-- PENDING → PROCESSING → FAILED
-- Any state → CANCELED

CREATE TYPE payment.payment_status AS ENUM (
    'PENDING',    -- Payment record created, awaiting processing
    'PROCESSING', -- Payment being processed by gateway
    'SUCCESS',    -- Payment completed successfully
    'FAILED',     -- Payment processing failed
    'CANCELED'    -- Payment canceled (booking canceled before payment)
);

COMMENT ON TYPE payment.payment_status IS 'Status of commission payment to referring user';

-- ============================================================================
-- GRANT PERMISSIONS
-- ============================================================================
-- Grant usage to application user (adjust username as needed)
-- GRANT USAGE ON SCHEMA payment TO gydi_app_user;
-- GRANT SELECT, INSERT, UPDATE ON ALL TABLES IN SCHEMA payment TO gydi_app_user;
-- GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA payment TO gydi_app_user;

COMMIT;

-- ============================================================================
-- ROLLBACK REFERENCE
-- ============================================================================
-- To rollback this migration (manually):
-- DROP TYPE IF EXISTS payment.payment_status CASCADE;
-- DROP SCHEMA IF EXISTS payment CASCADE;
