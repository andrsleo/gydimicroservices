-- V63__add_status_to_payment_methods.sql
-- Purpose: Add status column to payment_methods for granular lifecycle management
-- Author: Database Architect
-- Date: 2026-01-15
-- Dependencies: V52__create_subscription_plans_and_tables.sql

BEGIN;

-- ============================================================================
-- CREATE PAYMENT METHOD STATUS ENUM
-- ============================================================================
-- More granular status management than simple boolean is_active
-- Supports payment method lifecycle: ACTIVE → EXPIRED/FAILED → REPLACED/INACTIVE

CREATE TYPE subscriptions.payment_method_status AS ENUM (
    'ACTIVE',    -- Available for payments
    'INACTIVE',  -- Soft deleted or deactivated by user
    'EXPIRED',   -- Card expired
    'FAILED',    -- Gateway validation failed
    'REPLACED'   -- Replaced by another payment method
);

COMMENT ON TYPE subscriptions.payment_method_status IS
'Payment method lifecycle status. ACTIVE=available, INACTIVE=soft deleted, EXPIRED=card expired, FAILED=gateway validation failed, REPLACED=superseded by new method.';

-- ============================================================================
-- ADD STATUS COLUMN TO PAYMENT_METHODS
-- ============================================================================

-- Add the status column with NOT NULL constraint and default value
ALTER TABLE subscriptions.payment_methods
    ADD COLUMN status subscriptions.payment_method_status NOT NULL DEFAULT 'ACTIVE';

COMMENT ON COLUMN subscriptions.payment_methods.status IS
'Current status of payment method. Replaces is_active with more granular lifecycle management.';

-- ============================================================================
-- MIGRATE EXISTING DATA
-- ============================================================================
-- Map existing is_active values to new status field
-- is_active=true  → status='ACTIVE'
-- is_active=false → status='INACTIVE'

UPDATE subscriptions.payment_methods
SET status = CASE
    WHEN is_active = TRUE THEN 'ACTIVE'::subscriptions.payment_method_status
    ELSE 'INACTIVE'::subscriptions.payment_method_status
END;

-- ============================================================================
-- ADD INDEX FOR STATUS QUERIES
-- ============================================================================
-- Replace existing index that uses is_active with one that uses status

-- Drop old index
DROP INDEX IF EXISTS subscriptions.idx_payment_methods_user;

-- Create new index with status
CREATE INDEX idx_payment_methods_user_status
ON subscriptions.payment_methods(user_id, status, deleted_at);

COMMENT ON INDEX subscriptions.idx_payment_methods_user_status IS
'Optimized index for finding payment methods by user and status. Used by findActiveByUserId queries.';

-- ============================================================================
-- UPDATE DEFAULT PAYMENT METHOD INDEX
-- ============================================================================
-- Update index to use status instead of is_active

-- Drop old index
DROP INDEX IF EXISTS subscriptions.idx_payment_methods_default;

-- Create new index with status
CREATE INDEX idx_payment_methods_default_active
ON subscriptions.payment_methods(user_id, is_default, status)
WHERE is_default = TRUE AND status = 'ACTIVE' AND deleted_at IS NULL;

COMMENT ON INDEX subscriptions.idx_payment_methods_default_active IS
'Index for finding default active payment method per user. Excludes soft-deleted methods.';

-- ============================================================================
-- ADD CHECK CONSTRAINT
-- ============================================================================
-- Ensure business rules for status field

ALTER TABLE subscriptions.payment_methods
    ADD CONSTRAINT chk_payment_method_status_deleted CHECK (
        (status = 'INACTIVE' AND (deleted_at IS NOT NULL OR is_active = FALSE)) OR
        (status != 'INACTIVE')
    );

COMMENT ON CONSTRAINT chk_payment_method_status_deleted ON subscriptions.payment_methods IS
'Ensures INACTIVE status is consistent with deleted_at or is_active=false.';

-- ============================================================================
-- DEPRECATION NOTICE FOR IS_ACTIVE
-- ============================================================================
-- The is_active column is now deprecated in favor of status
-- Kept for backward compatibility but should not be used in new code
-- Will be removed in a future migration after all code is migrated

COMMENT ON COLUMN subscriptions.payment_methods.is_active IS
'DEPRECATED: Use status column instead. Kept for backward compatibility only. Will be removed in future version.';

-- ============================================================================
-- HELPER FUNCTION: UPDATE STATUS ON EXPIRY
-- ============================================================================
-- Function to automatically mark expired cards

CREATE OR REPLACE FUNCTION subscriptions.mark_expired_payment_methods()
RETURNS TABLE(expired_count INTEGER) AS $$
DECLARE
    updated_rows INTEGER;
BEGIN
    WITH updated AS (
        UPDATE subscriptions.payment_methods
        SET status = 'EXPIRED',
            is_active = FALSE,
            updated_at = CURRENT_TIMESTAMP
        WHERE status = 'ACTIVE'
          AND method_type IN ('CREDIT_CARD', 'DEBIT_CARD')
          AND deleted_at IS NULL
          AND (
              (card_expiry_year < EXTRACT(YEAR FROM CURRENT_DATE)) OR
              (card_expiry_year = EXTRACT(YEAR FROM CURRENT_DATE) AND card_expiry_month < EXTRACT(MONTH FROM CURRENT_DATE))
          )
        RETURNING id
    )
    SELECT COUNT(*)::INTEGER INTO updated_rows FROM updated;

    RAISE NOTICE 'Marked % expired payment methods', updated_rows;
    RETURN QUERY SELECT updated_rows;
END;
$$ LANGUAGE plpgsql;

COMMENT ON FUNCTION subscriptions.mark_expired_payment_methods IS
'Marks credit/debit cards as EXPIRED based on expiry_month and expiry_year. Run monthly: SELECT * FROM subscriptions.mark_expired_payment_methods();';

COMMIT;

-- ============================================================================
-- VERIFICATION QUERIES (for testing - NOT executed)
-- ============================================================================
-- Verify migration:
-- SELECT user_id, method_type, status, is_active, deleted_at FROM subscriptions.payment_methods;
--
-- Check for ACTIVE payment methods:
-- SELECT COUNT(*) FROM subscriptions.payment_methods WHERE status = 'ACTIVE';
--
-- Test expiry function:
-- SELECT * FROM subscriptions.mark_expired_payment_methods();

-- ============================================================================
-- ROLLBACK REFERENCE (for manual rollback only - NOT executed)
-- ============================================================================
-- DROP FUNCTION IF EXISTS subscriptions.mark_expired_payment_methods();
-- ALTER TABLE subscriptions.payment_methods DROP CONSTRAINT IF EXISTS chk_payment_method_status_deleted;
-- DROP INDEX IF EXISTS subscriptions.idx_payment_methods_default_active;
-- DROP INDEX IF EXISTS subscriptions.idx_payment_methods_user_status;
-- ALTER TABLE subscriptions.payment_methods DROP COLUMN IF EXISTS status;
-- DROP TYPE IF EXISTS subscriptions.payment_method_status;
