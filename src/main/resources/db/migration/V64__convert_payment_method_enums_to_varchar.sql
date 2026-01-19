-- V64__convert_payment_method_enums_to_varchar.sql
-- Purpose: Convert ENUM columns in payment_methods to VARCHAR for Hibernate compatibility
-- Author: Database Architect
-- Date: 2026-01-15
-- Dependencies: V52__create_subscription_plans_and_tables.sql, V63__add_status_to_payment_methods.sql

BEGIN;

-- ============================================================================
-- CONVERT method_type FROM ENUM TO VARCHAR
-- ============================================================================

-- Step 1: Add temporary column
ALTER TABLE subscriptions.payment_methods
    ADD COLUMN method_type_temp VARCHAR(50);

-- Step 2: Copy data with explicit cast
UPDATE subscriptions.payment_methods
SET method_type_temp = method_type::TEXT;

-- Step 3: Drop old column
ALTER TABLE subscriptions.payment_methods
    DROP COLUMN method_type;

-- Step 4: Rename temp column
ALTER TABLE subscriptions.payment_methods
    RENAME COLUMN method_type_temp TO method_type;

-- Step 5: Add NOT NULL constraint
ALTER TABLE subscriptions.payment_methods
    ALTER COLUMN method_type SET NOT NULL;

-- Step 6: Set default value
ALTER TABLE subscriptions.payment_methods
    ALTER COLUMN method_type SET DEFAULT 'CREDIT_CARD';

-- Step 7: Add CHECK constraint to enforce valid values
ALTER TABLE subscriptions.payment_methods
    ADD CONSTRAINT chk_payment_method_type_values
    CHECK (method_type IN ('CREDIT_CARD', 'DEBIT_CARD', 'PAYPAL', 'STRIPE', 'OTHER'));

COMMENT ON COLUMN subscriptions.payment_methods.method_type IS
'Payment method type as VARCHAR for Hibernate compatibility. Valid values: CREDIT_CARD, DEBIT_CARD, PAYPAL, STRIPE, OTHER.';

-- ============================================================================
-- CONVERT status FROM ENUM TO VARCHAR
-- ============================================================================

-- Step 1: Add temporary column
ALTER TABLE subscriptions.payment_methods
    ADD COLUMN status_temp VARCHAR(20);

-- Step 2: Copy data with explicit cast
UPDATE subscriptions.payment_methods
SET status_temp = status::TEXT;

-- Step 3: Drop old column and its constraint
ALTER TABLE subscriptions.payment_methods
    DROP CONSTRAINT IF EXISTS chk_payment_method_status_deleted;

ALTER TABLE subscriptions.payment_methods
    DROP COLUMN status;

-- Step 4: Rename temp column
ALTER TABLE subscriptions.payment_methods
    RENAME COLUMN status_temp TO status;

-- Step 5: Add NOT NULL constraint
ALTER TABLE subscriptions.payment_methods
    ALTER COLUMN status SET NOT NULL;

-- Step 6: Set default value
ALTER TABLE subscriptions.payment_methods
    ALTER COLUMN status SET DEFAULT 'ACTIVE';

-- Step 7: Add CHECK constraint to enforce valid values
ALTER TABLE subscriptions.payment_methods
    ADD CONSTRAINT chk_payment_method_status_values
    CHECK (status IN ('ACTIVE', 'INACTIVE', 'EXPIRED', 'FAILED', 'REPLACED'));

-- Step 8: Re-add business rule constraint
ALTER TABLE subscriptions.payment_methods
    ADD CONSTRAINT chk_payment_method_status_deleted CHECK (
        (status = 'INACTIVE' AND (deleted_at IS NOT NULL OR is_active = FALSE)) OR
        (status != 'INACTIVE')
    );

COMMENT ON COLUMN subscriptions.payment_methods.status IS
'Payment method status as VARCHAR for Hibernate compatibility. Valid values: ACTIVE, INACTIVE, EXPIRED, FAILED, REPLACED.';

-- ============================================================================
-- RECREATE INDEXES THAT REFERENCED THE OLD ENUM COLUMNS
-- ============================================================================

-- Drop old indexes that referenced ENUM types
DROP INDEX IF EXISTS subscriptions.idx_payment_methods_expiring;
DROP INDEX IF EXISTS subscriptions.idx_payment_methods_default_active;

-- Recreate indexes with VARCHAR columns
CREATE INDEX idx_payment_methods_expiring
ON subscriptions.payment_methods(card_expiry_year, card_expiry_month)
WHERE deleted_at IS NULL AND method_type IN ('CREDIT_CARD', 'DEBIT_CARD');

CREATE INDEX idx_payment_methods_default_active
ON subscriptions.payment_methods(user_id, is_default, status)
WHERE is_default = TRUE AND status = 'ACTIVE' AND deleted_at IS NULL;

COMMENT ON INDEX subscriptions.idx_payment_methods_expiring IS
'Index for finding expiring credit/debit cards (VARCHAR-based).';

COMMENT ON INDEX subscriptions.idx_payment_methods_default_active IS
'Index for finding default active payment method per user (VARCHAR-based).';

-- ============================================================================
-- UPDATE CHECK CONSTRAINTS THAT REFERENCED OLD ENUM COLUMNS
-- ============================================================================

-- Drop old check constraints that referenced ENUM types
ALTER TABLE subscriptions.payment_methods
    DROP CONSTRAINT IF EXISTS chk_card_details;

ALTER TABLE subscriptions.payment_methods
    DROP CONSTRAINT IF EXISTS chk_paypal_email;

-- Recreate check constraints with VARCHAR comparisons
ALTER TABLE subscriptions.payment_methods
    ADD CONSTRAINT chk_card_details CHECK (
        (method_type = 'CREDIT_CARD' AND card_last_four IS NOT NULL) OR
        (method_type = 'DEBIT_CARD' AND card_last_four IS NOT NULL) OR
        (method_type NOT IN ('CREDIT_CARD', 'DEBIT_CARD'))
    );

ALTER TABLE subscriptions.payment_methods
    ADD CONSTRAINT chk_paypal_email CHECK (
        (method_type = 'PAYPAL' AND paypal_email IS NOT NULL) OR
        (method_type != 'PAYPAL')
    );

COMMENT ON CONSTRAINT chk_card_details ON subscriptions.payment_methods IS
'Ensures credit/debit cards have last four digits (VARCHAR-based).';

COMMENT ON CONSTRAINT chk_paypal_email ON subscriptions.payment_methods IS
'Ensures PayPal payment methods have email address (VARCHAR-based).';

-- ============================================================================
-- UPDATE HELPER FUNCTION
-- ============================================================================

-- Drop and recreate function with VARCHAR logic
DROP FUNCTION IF EXISTS subscriptions.mark_expired_payment_methods();

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
'Marks credit/debit cards as EXPIRED based on expiry date. Uses VARCHAR status field. Run monthly: SELECT * FROM subscriptions.mark_expired_payment_methods();';

-- ============================================================================
-- DROP ENUM TYPES (now unused)
-- ============================================================================
-- Note: We can only drop the enum types if they're not referenced elsewhere
-- First check if any other tables use these types

DO $$
BEGIN
    -- Check if payment_method_type enum is used elsewhere
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'subscriptions'
          AND udt_name = 'payment_method_type'
          AND table_name != 'payment_methods'
    ) THEN
        DROP TYPE IF EXISTS subscriptions.payment_method_type;
        RAISE NOTICE 'Dropped unused enum type: subscriptions.payment_method_type';
    ELSE
        RAISE NOTICE 'Cannot drop subscriptions.payment_method_type - still in use by other tables';
    END IF;

    -- Check if payment_method_status enum is used elsewhere
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'subscriptions'
          AND udt_name = 'payment_method_status'
          AND table_name != 'payment_methods'
    ) THEN
        DROP TYPE IF EXISTS subscriptions.payment_method_status;
        RAISE NOTICE 'Dropped unused enum type: subscriptions.payment_method_status';
    ELSE
        RAISE NOTICE 'Cannot drop subscriptions.payment_method_status - still in use by other tables';
    END IF;
END $$;

COMMIT;

-- ============================================================================
-- VERIFICATION QUERIES (for testing - NOT executed)
-- ============================================================================
-- Verify conversion:
-- SELECT column_name, data_type, character_maximum_length
-- FROM information_schema.columns
-- WHERE table_schema = 'subscriptions' AND table_name = 'payment_methods'
-- AND column_name IN ('method_type', 'status');
--
-- Check data integrity:
-- SELECT method_type, status, COUNT(*)
-- FROM subscriptions.payment_methods
-- GROUP BY method_type, status;
--
-- Test constraints:
-- INSERT INTO subscriptions.payment_methods (user_id, method_type, gateway_provider, gateway_token, status)
-- VALUES (1, 'INVALID_TYPE', 'test', 'test_token', 'ACTIVE');  -- Should fail
--
-- Test function:
-- SELECT * FROM subscriptions.mark_expired_payment_methods();

-- ============================================================================
-- ROLLBACK REFERENCE (for manual rollback only - NOT executed)
-- ============================================================================
-- WARNING: This rollback recreates ENUM types - may fail if data has invalid values
--
-- CREATE TYPE subscriptions.payment_method_type AS ENUM ('CREDIT_CARD', 'DEBIT_CARD', 'PAYPAL', 'STRIPE', 'OTHER');
-- CREATE TYPE subscriptions.payment_method_status AS ENUM ('ACTIVE', 'INACTIVE', 'EXPIRED', 'FAILED', 'REPLACED');
--
-- ALTER TABLE subscriptions.payment_methods ALTER COLUMN method_type TYPE subscriptions.payment_method_type USING method_type::subscriptions.payment_method_type;
-- ALTER TABLE subscriptions.payment_methods ALTER COLUMN status TYPE subscriptions.payment_method_status USING status::subscriptions.payment_method_status;
