-- V93__migrate_payment_methods_to_payment_schema.sql
-- Purpose: Move payment_methods table from subscriptions schema to payment schema
--
-- Rationale: payment_methods semantically belong to the payment bounded context,
--   not to subscriptions. This migration relocates the table while preserving all
--   data, constraints, indexes, triggers, and functions.
--
-- Author: Database Architect
-- Date: 2026-03-04
-- Dependencies:
--   V52__create_subscription_plans_and_tables.sql  (original table creation)
--   V57__add_billing_email_to_payment_methods.sql  (billing_email column)
--   V63__add_status_to_payment_methods.sql         (status column + enum type)
--   V64__convert_payment_method_enums_to_varchar.sql (VARCHAR conversion)
--   V65__fix_column_types_for_railway.sql           (card_last_four VARCHAR(4))
--   V81__enhance_payment_methods_for_commissions.sql (purpose, verified_at, verification_status)
--   V41__create_payment_schema.sql                  (payment schema already exists)
--
-- Impact:
--   - Table: subscriptions.payment_methods → payment.payment_methods
--   - FKs updated:
--       subscriptions.user_subscriptions.payment_method_id
--       subscriptions.subscription_transactions.payment_method_id
--   - ENUM type moved:
--       subscriptions.payment_method_purpose → payment.payment_method_purpose
--   - Functions moved to payment schema:
--       subscriptions.ensure_single_default_payment_method()
--       subscriptions.mark_expired_payment_methods()
--   - Function updated to reference new location:
--       subscriptions.update_payment_method_purpose_to_host()
--   - Trigger recreated in payment schema:
--       ensure_single_default_payment_method_trigger
--       update_payment_methods_updated_at
--
-- Safety guarantees:
--   - Entire migration wrapped in a single transaction (DDL is transactional in PG)
--   - IF EXISTS guards on all DROP statements
--   - Data preserved via INSERT ... SELECT before DROP
--   - FK constraints re-created before old table is dropped
-- ============================================================================

BEGIN;

-- ============================================================================
-- STEP 1: MOVE ENUM TYPE payment_method_purpose TO payment SCHEMA
-- ============================================================================
-- The purpose ENUM was created in subscriptions schema (V81). We create an
-- equivalent type in the payment schema so the new table can reference it.
-- We cannot ALTER TYPE ... SET SCHEMA while it has dependent columns, so we
-- create a new type in payment schema, migrate the column, then drop the old type.

-- 1a. Create the equivalent ENUM in payment schema (idempotent via DO $$)
-- PostgreSQL does not support CREATE TYPE IF NOT EXISTS for ENUM types; we use
-- a DO $$ block with a pg_type existence check instead.
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM   pg_type     t
        JOIN   pg_namespace n ON t.typnamespace = n.oid
        WHERE  n.nspname = 'payment'
          AND  t.typname = 'payment_method_purpose'
    ) THEN
        CREATE TYPE payment.payment_method_purpose AS ENUM (
            'SUBSCRIPTION',
            'HOST_COMMISSION',
            'BOTH'
        );
    END IF;
END $$;

COMMENT ON TYPE payment.payment_method_purpose IS
'Indicates the purpose of a payment method. SUBSCRIPTION=subscription billing only, HOST_COMMISSION=host commission charges only, BOTH=shared for both purposes.';


-- ============================================================================
-- STEP 2: CREATE payment.payment_methods WITH FULL STRUCTURE
-- ============================================================================
-- Replicate the exact final structure of subscriptions.payment_methods after
-- all previous migrations (V52 through V81).

CREATE TABLE IF NOT EXISTS payment.payment_methods (
    -- Primary key
    id BIGSERIAL PRIMARY KEY,

    -- Foreign keys
    user_id BIGINT NOT NULL,

    -- Payment method type (VARCHAR after V64)
    method_type VARCHAR(50) NOT NULL DEFAULT 'CREDIT_CARD',

    -- Tokenized payment data (from payment gateway)
    gateway_provider VARCHAR(50) NOT NULL,
    gateway_token    VARCHAR(255) NOT NULL,

    -- Display information (NEVER full card number)
    card_last_four   VARCHAR(4),     -- VARCHAR(4) after V65
    card_brand       VARCHAR(50),
    card_expiry_month INT CHECK (card_expiry_month >= 1 AND card_expiry_month <= 12),
    card_expiry_year  INT CHECK (card_expiry_year >= 2025),

    -- PayPal details
    paypal_email     VARCHAR(255),

    -- Billing address
    billing_name        VARCHAR(255),
    billing_country     VARCHAR(2),
    billing_postal_code VARCHAR(20),

    -- Billing email (added in V57)
    billing_email VARCHAR(255),

    -- Status flags
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    is_active  BOOLEAN NOT NULL DEFAULT TRUE, -- DEPRECATED: use status

    -- Status column (VARCHAR after V64)
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',

    -- Purpose column (moved from subscriptions.payment_method_purpose)
    purpose payment.payment_method_purpose NOT NULL DEFAULT 'SUBSCRIPTION',

    -- Verification fields (added in V81)
    verified_at           TIMESTAMP WITH TIME ZONE,
    verification_status   VARCHAR(20) DEFAULT 'PENDING'
        CHECK (verification_status IN ('PENDING', 'VERIFIED', 'FAILED')),

    -- Soft delete
    deleted_at TIMESTAMP WITH TIME ZONE,

    -- Audit timestamps
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Unique and check constraints
    CONSTRAINT chk_pm_gateway_token_unique
        UNIQUE (gateway_provider, gateway_token),

    CONSTRAINT chk_pm_payment_method_type_values
        CHECK (method_type IN ('CREDIT_CARD', 'DEBIT_CARD', 'PAYPAL', 'STRIPE', 'OTHER')),

    CONSTRAINT chk_pm_payment_method_status_values
        CHECK (status IN ('ACTIVE', 'INACTIVE', 'EXPIRED', 'FAILED', 'REPLACED')),

    CONSTRAINT chk_pm_payment_method_status_deleted
        CHECK (
            (status = 'INACTIVE' AND (deleted_at IS NOT NULL OR is_active = FALSE))
            OR (status != 'INACTIVE')
        ),

    CONSTRAINT chk_pm_card_details
        CHECK (
            (method_type = 'CREDIT_CARD' AND card_last_four IS NOT NULL)
            OR (method_type = 'DEBIT_CARD' AND card_last_four IS NOT NULL)
            OR (method_type NOT IN ('CREDIT_CARD', 'DEBIT_CARD'))
        ),

    CONSTRAINT chk_pm_paypal_email
        CHECK (
            (method_type = 'PAYPAL' AND paypal_email IS NOT NULL)
            OR (method_type != 'PAYPAL')
        ),

    -- Foreign key to users
    CONSTRAINT fk_pm_user
        FOREIGN KEY (user_id)
        REFERENCES users.users(id)
        ON DELETE CASCADE
);

COMMENT ON TABLE payment.payment_methods IS
'Tokenized payment methods for users. NEVER stores raw card numbers. Supports soft delete (deleted_at). Multiple methods per user allowed. Moved from subscriptions schema in V93.';

COMMENT ON COLUMN payment.payment_methods.gateway_token IS
'Tokenized payment method ID from gateway (e.g., Stripe payment method ID). NEVER store raw card numbers.';

COMMENT ON COLUMN payment.payment_methods.deleted_at IS
'Soft delete timestamp. Payment methods are NEVER physically deleted for audit purposes.';

COMMENT ON COLUMN payment.payment_methods.is_active IS
'DEPRECATED: Use status column instead. Kept for backward compatibility only.';

COMMENT ON COLUMN payment.payment_methods.purpose IS
'Purpose of this payment method. SUBSCRIPTION=subscription auto-billing, HOST_COMMISSION=host commission charges, BOTH=shared for both purposes. Defaults to SUBSCRIPTION for backward compatibility.';

COMMENT ON COLUMN payment.payment_methods.verified_at IS
'Timestamp when payment method was verified (e.g., via $1 authorization test). NULL if not yet verified.';

COMMENT ON COLUMN payment.payment_methods.verification_status IS
'Verification status of payment method. PENDING=awaiting verification, VERIFIED=successfully verified, FAILED=verification failed.';


-- ============================================================================
-- STEP 3: MIGRATE DATA FROM subscriptions.payment_methods TO payment.payment_methods
-- ============================================================================

-- Only migrate data if the source table still exists. When V94 is re-applied
-- after a partial execution, subscriptions.payment_methods will already be gone
-- (dropped in STEP 9) and the data will already be in payment.payment_methods.
-- ON CONFLICT DO NOTHING handles duplicate rows in case some were inserted before
-- the script failed on a previous attempt.
DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM   information_schema.tables
        WHERE  table_schema = 'subscriptions'
          AND  table_name   = 'payment_methods'
    ) THEN
        INSERT INTO payment.payment_methods (
            id,
            user_id,
            method_type,
            gateway_provider,
            gateway_token,
            card_last_four,
            card_brand,
            card_expiry_month,
            card_expiry_year,
            paypal_email,
            billing_name,
            billing_country,
            billing_postal_code,
            billing_email,
            is_default,
            is_active,
            status,
            purpose,
            verified_at,
            verification_status,
            deleted_at,
            created_at,
            updated_at
        )
        SELECT
            id,
            user_id,
            method_type,
            gateway_provider,
            gateway_token,
            card_last_four,
            card_brand,
            card_expiry_month,
            card_expiry_year,
            paypal_email,
            billing_name,
            billing_country,
            billing_postal_code,
            billing_email,
            is_default,
            is_active,
            status,
            -- Cast from subscriptions.payment_method_purpose to payment.payment_method_purpose
            purpose::text::payment.payment_method_purpose,
            verified_at,
            verification_status,
            deleted_at,
            created_at,
            updated_at
        FROM subscriptions.payment_methods
        ON CONFLICT (id) DO NOTHING;

        RAISE NOTICE 'Data migrated from subscriptions.payment_methods to payment.payment_methods';
    ELSE
        RAISE NOTICE 'subscriptions.payment_methods does not exist — skipping data migration (already done)';
    END IF;
END $$;

-- Reset the sequence to match the max id from migrated data (avoid PK collisions).
-- Wrapped in DO $$ to guard against the table not yet having a sequence row.
DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM   information_schema.tables
        WHERE  table_schema = 'payment'
          AND  table_name   = 'payment_methods'
    ) THEN
        PERFORM setval(
            'payment.payment_methods_id_seq',
            COALESCE((SELECT MAX(id) FROM payment.payment_methods), 1),
            true
        );
    END IF;
END $$;


-- ============================================================================
-- STEP 4: DROP FOREIGN KEYS FROM subscriptions TABLES THAT POINT TO OLD TABLE
-- ============================================================================

-- FK in subscriptions.user_subscriptions
ALTER TABLE subscriptions.user_subscriptions
    DROP CONSTRAINT IF EXISTS fk_user_subscription_payment_method;

-- FK in subscriptions.subscription_transactions
ALTER TABLE subscriptions.subscription_transactions
    DROP CONSTRAINT IF EXISTS fk_transaction_payment_method;


-- ============================================================================
-- STEP 5: RECREATE FOREIGN KEYS POINTING TO payment.payment_methods
-- ============================================================================

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM   information_schema.table_constraints
        WHERE  constraint_schema = 'subscriptions'
          AND  table_name        = 'user_subscriptions'
          AND  constraint_name   = 'fk_user_subscription_payment_method'
    ) THEN
        ALTER TABLE subscriptions.user_subscriptions
            ADD CONSTRAINT fk_user_subscription_payment_method
            FOREIGN KEY (payment_method_id)
            REFERENCES payment.payment_methods(id)
            ON DELETE SET NULL;
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM   information_schema.table_constraints
        WHERE  constraint_schema = 'subscriptions'
          AND  table_name        = 'subscription_transactions'
          AND  constraint_name   = 'fk_transaction_payment_method'
    ) THEN
        ALTER TABLE subscriptions.subscription_transactions
            ADD CONSTRAINT fk_transaction_payment_method
            FOREIGN KEY (payment_method_id)
            REFERENCES payment.payment_methods(id)
            ON DELETE SET NULL;
    END IF;
END $$;


-- ============================================================================
-- STEP 6: RECREATE INDEXES ON payment.payment_methods
-- ============================================================================

-- General lookup by user + status
CREATE INDEX IF NOT EXISTS idx_payment_methods_user_status
    ON payment.payment_methods(user_id, status, deleted_at);

COMMENT ON INDEX payment.idx_payment_methods_user_status IS
'Optimized index for finding payment methods by user and status.';

-- Default active payment method per user
CREATE INDEX IF NOT EXISTS idx_payment_methods_default_active
    ON payment.payment_methods(user_id, is_default, status)
    WHERE is_default = TRUE AND status = 'ACTIVE' AND deleted_at IS NULL;

COMMENT ON INDEX payment.idx_payment_methods_default_active IS
'Index for finding default active payment method per user.';

-- Expiring credit/debit cards
CREATE INDEX IF NOT EXISTS idx_payment_methods_expiring
    ON payment.payment_methods(card_expiry_year, card_expiry_month)
    WHERE deleted_at IS NULL AND method_type IN ('CREDIT_CARD', 'DEBIT_CARD');

COMMENT ON INDEX payment.idx_payment_methods_expiring IS
'Index for finding expiring credit/debit cards.';

-- Lookup by purpose (for commission processing)
CREATE INDEX IF NOT EXISTS idx_payment_methods_purpose
    ON payment.payment_methods(user_id, purpose, status)
    WHERE deleted_at IS NULL;

COMMENT ON INDEX payment.idx_payment_methods_purpose IS
'Index for finding payment methods by user and purpose (SUBSCRIPTION, HOST_COMMISSION, BOTH).';


-- ============================================================================
-- STEP 7: CREATE TRIGGERS AND FUNCTIONS IN payment SCHEMA
-- ============================================================================

-- 7a. updated_at trigger function (payment schema version)
-- Note: subscriptions.update_updated_at_column() is kept intact for other
--       subscriptions tables (subscription_plans, user_subscriptions).
--       We create a dedicated function in payment schema.

CREATE OR REPLACE FUNCTION payment.update_payment_methods_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

COMMENT ON FUNCTION payment.update_payment_methods_updated_at IS
'Updates updated_at timestamp on payment.payment_methods rows.';

DROP TRIGGER IF EXISTS update_payment_methods_updated_at
    ON payment.payment_methods;

CREATE TRIGGER update_payment_methods_updated_at
    BEFORE UPDATE ON payment.payment_methods
    FOR EACH ROW EXECUTE FUNCTION payment.update_payment_methods_updated_at();


-- 7b. Single default payment method trigger (payment schema version)

CREATE OR REPLACE FUNCTION payment.ensure_single_default_payment_method()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.is_default = TRUE THEN
        UPDATE payment.payment_methods
        SET is_default = FALSE
        WHERE user_id = NEW.user_id
          AND id != NEW.id
          AND is_default = TRUE;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

COMMENT ON FUNCTION payment.ensure_single_default_payment_method IS
'Ensures only one payment method per user is marked as default.';

DROP TRIGGER IF EXISTS ensure_single_default_payment_method_trigger
    ON payment.payment_methods;

CREATE TRIGGER ensure_single_default_payment_method_trigger
    BEFORE INSERT OR UPDATE ON payment.payment_methods
    FOR EACH ROW EXECUTE FUNCTION payment.ensure_single_default_payment_method();


-- 7c. mark_expired_payment_methods function (recreated to reference payment schema)

CREATE OR REPLACE FUNCTION payment.mark_expired_payment_methods()
RETURNS TABLE(expired_count INTEGER) AS $$
DECLARE
    updated_rows INTEGER;
BEGIN
    WITH updated AS (
        UPDATE payment.payment_methods
        SET status = 'EXPIRED',
            is_active = FALSE,
            updated_at = CURRENT_TIMESTAMP
        WHERE status = 'ACTIVE'
          AND method_type IN ('CREDIT_CARD', 'DEBIT_CARD')
          AND deleted_at IS NULL
          AND (
              (card_expiry_year < EXTRACT(YEAR FROM CURRENT_DATE)) OR
              (card_expiry_year = EXTRACT(YEAR FROM CURRENT_DATE)
               AND card_expiry_month < EXTRACT(MONTH FROM CURRENT_DATE))
          )
        RETURNING id
    )
    SELECT COUNT(*)::INTEGER INTO updated_rows FROM updated;

    RAISE NOTICE 'Marked % expired payment methods', updated_rows;
    RETURN QUERY SELECT updated_rows;
END;
$$ LANGUAGE plpgsql;

COMMENT ON FUNCTION payment.mark_expired_payment_methods IS
'Marks credit/debit cards as EXPIRED based on expiry date. Run monthly: SELECT * FROM payment.mark_expired_payment_methods();';


-- ============================================================================
-- STEP 8: UPDATE subscriptions.update_payment_method_purpose_to_host()
--         TO REFERENCE payment.payment_methods
-- ============================================================================
-- This function was created in V81 in the subscriptions schema and references
-- subscriptions.payment_methods. We replace it to point to payment.payment_methods.

CREATE OR REPLACE FUNCTION subscriptions.update_payment_method_purpose_to_host(
    p_user_id BIGINT
)
RETURNS VOID AS $$
BEGIN
    -- Update default payment method to BOTH (SUBSCRIPTION + HOST_COMMISSION)
    UPDATE payment.payment_methods
    SET purpose = 'BOTH',
        updated_at = CURRENT_TIMESTAMP
    WHERE user_id = p_user_id
      AND is_default = TRUE
      AND status = 'ACTIVE'
      AND deleted_at IS NULL
      AND purpose = 'SUBSCRIPTION';

    -- Update onboarding status
    INSERT INTO users.user_onboarding_status (
        user_id,
        host_payment_method_added,
        host_payment_method_verified,
        host_payment_method_added_at
    ) VALUES (
        p_user_id,
        TRUE,
        TRUE,
        CURRENT_TIMESTAMP
    )
    ON CONFLICT (user_id) DO UPDATE
    SET host_payment_method_added        = TRUE,
        host_payment_method_verified     = TRUE,
        host_payment_method_added_at     = CURRENT_TIMESTAMP,
        updated_at                       = CURRENT_TIMESTAMP;

    RAISE NOTICE 'Updated payment method purpose to BOTH for user %', p_user_id;
END;
$$ LANGUAGE plpgsql;

COMMENT ON FUNCTION subscriptions.update_payment_method_purpose_to_host IS
'Updates default payment method purpose from SUBSCRIPTION to BOTH when user publishes first property. References payment.payment_methods (updated in V93).';


-- ============================================================================
-- STEP 9: DROP OLD TABLE AND CLEANUP IN subscriptions SCHEMA
-- ============================================================================

-- 9a. Drop triggers that depend on subscriptions.payment_methods
DROP TRIGGER IF EXISTS update_payment_methods_updated_at
    ON subscriptions.payment_methods;

DROP TRIGGER IF EXISTS ensure_single_default_payment_method_trigger
    ON subscriptions.payment_methods;

-- 9b. Drop the old table (cascades any remaining indexes and constraints)
DROP TABLE IF EXISTS subscriptions.payment_methods CASCADE;

-- 9c. Drop functions that existed only for subscriptions.payment_methods
DROP FUNCTION IF EXISTS subscriptions.ensure_single_default_payment_method();
DROP FUNCTION IF EXISTS subscriptions.mark_expired_payment_methods();

-- 9d. Drop old ENUM type now that no columns reference it
DROP TYPE IF EXISTS subscriptions.payment_method_purpose;


-- ============================================================================
-- STEP 10: VERIFICATION
-- ============================================================================

DO $$
DECLARE
    new_table_exists      BOOLEAN;
    old_table_gone        BOOLEAN;
    row_count             BIGINT;
    fk_us_exists          BOOLEAN;
    fk_tx_exists          BOOLEAN;
    purpose_type_exists   BOOLEAN;
    old_purpose_gone      BOOLEAN;
BEGIN
    -- Verify new table exists
    SELECT EXISTS (
        SELECT 1 FROM information_schema.tables
        WHERE table_schema = 'payment' AND table_name = 'payment_methods'
    ) INTO new_table_exists;

    -- Verify old table is gone
    SELECT NOT EXISTS (
        SELECT 1 FROM information_schema.tables
        WHERE table_schema = 'subscriptions' AND table_name = 'payment_methods'
    ) INTO old_table_gone;

    -- Count rows in new table
    SELECT COUNT(*) INTO row_count FROM payment.payment_methods;

    -- Verify FK from user_subscriptions points to payment schema
    SELECT EXISTS (
        SELECT 1
        FROM information_schema.referential_constraints rc
        JOIN information_schema.table_constraints tc
          ON rc.constraint_name = tc.constraint_name
         AND tc.constraint_schema = 'subscriptions'
         AND tc.table_name = 'user_subscriptions'
        JOIN information_schema.table_constraints tc2
          ON rc.unique_constraint_name = tc2.constraint_name
         AND tc2.constraint_schema = 'payment'
         AND tc2.table_name = 'payment_methods'
        WHERE rc.constraint_name = 'fk_user_subscription_payment_method'
    ) INTO fk_us_exists;

    -- Verify FK from subscription_transactions points to payment schema
    SELECT EXISTS (
        SELECT 1
        FROM information_schema.referential_constraints rc
        JOIN information_schema.table_constraints tc
          ON rc.constraint_name = tc.constraint_name
         AND tc.constraint_schema = 'subscriptions'
         AND tc.table_name = 'subscription_transactions'
        JOIN information_schema.table_constraints tc2
          ON rc.unique_constraint_name = tc2.constraint_name
         AND tc2.constraint_schema = 'payment'
         AND tc2.table_name = 'payment_methods'
        WHERE rc.constraint_name = 'fk_transaction_payment_method'
    ) INTO fk_tx_exists;

    -- Verify new ENUM type exists in payment schema
    SELECT EXISTS (
        SELECT 1 FROM pg_type t
        JOIN pg_namespace n ON t.typnamespace = n.oid
        WHERE n.nspname = 'payment' AND t.typname = 'payment_method_purpose'
    ) INTO purpose_type_exists;

    -- Verify old ENUM type is gone from subscriptions schema
    SELECT NOT EXISTS (
        SELECT 1 FROM pg_type t
        JOIN pg_namespace n ON t.typnamespace = n.oid
        WHERE n.nspname = 'subscriptions' AND t.typname = 'payment_method_purpose'
    ) INTO old_purpose_gone;

    IF NOT new_table_exists THEN
        RAISE EXCEPTION 'FAILED: payment.payment_methods table was not created';
    END IF;

    IF NOT old_table_gone THEN
        RAISE EXCEPTION 'FAILED: subscriptions.payment_methods table was not dropped';
    END IF;

    IF NOT purpose_type_exists THEN
        RAISE EXCEPTION 'FAILED: payment.payment_method_purpose ENUM type was not created';
    END IF;

    IF NOT old_purpose_gone THEN
        RAISE EXCEPTION 'FAILED: subscriptions.payment_method_purpose ENUM type was not dropped';
    END IF;

    RAISE NOTICE '';
    RAISE NOTICE '✅ V93 Migration successful:';
    RAISE NOTICE '   ✓ payment.payment_methods table created';
    RAISE NOTICE '   ✓ % rows migrated from subscriptions.payment_methods', row_count;
    RAISE NOTICE '   ✓ subscriptions.payment_methods table dropped';
    RAISE NOTICE '   ✓ FK fk_user_subscription_payment_method re-pointed to payment schema: %', fk_us_exists;
    RAISE NOTICE '   ✓ FK fk_transaction_payment_method re-pointed to payment schema: %', fk_tx_exists;
    RAISE NOTICE '   ✓ payment.payment_method_purpose ENUM type created';
    RAISE NOTICE '   ✓ subscriptions.payment_method_purpose ENUM type dropped';
    RAISE NOTICE '   ✓ Triggers and functions migrated to payment schema';
    RAISE NOTICE '   ✓ subscriptions.update_payment_method_purpose_to_host() updated';
    RAISE NOTICE '';
END $$;

COMMIT;

-- ============================================================================
-- ROLLBACK REFERENCE (for manual rollback only - NOT auto-executed)
-- ============================================================================
-- WARNING: Run this only if you need to undo V93 manually.
--
-- BEGIN;
--
-- -- Re-create the ENUM type in subscriptions schema
-- CREATE TYPE subscriptions.payment_method_purpose AS ENUM ('SUBSCRIPTION', 'HOST_COMMISSION', 'BOTH');
--
-- -- Re-create the table in subscriptions schema
-- CREATE TABLE subscriptions.payment_methods (
--     id BIGSERIAL PRIMARY KEY,
--     user_id BIGINT NOT NULL,
--     method_type VARCHAR(50) NOT NULL DEFAULT 'CREDIT_CARD',
--     gateway_provider VARCHAR(50) NOT NULL,
--     gateway_token VARCHAR(255) NOT NULL,
--     card_last_four VARCHAR(4),
--     card_brand VARCHAR(50),
--     card_expiry_month INT CHECK (card_expiry_month >= 1 AND card_expiry_month <= 12),
--     card_expiry_year INT CHECK (card_expiry_year >= 2025),
--     paypal_email VARCHAR(255),
--     billing_name VARCHAR(255),
--     billing_country VARCHAR(2),
--     billing_postal_code VARCHAR(20),
--     billing_email VARCHAR(255),
--     is_default BOOLEAN NOT NULL DEFAULT FALSE,
--     is_active BOOLEAN NOT NULL DEFAULT TRUE,
--     status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
--     purpose subscriptions.payment_method_purpose NOT NULL DEFAULT 'SUBSCRIPTION',
--     verified_at TIMESTAMP WITH TIME ZONE,
--     verification_status VARCHAR(20) DEFAULT 'PENDING' CHECK (verification_status IN ('PENDING', 'VERIFIED', 'FAILED')),
--     deleted_at TIMESTAMP WITH TIME ZONE,
--     created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
--     updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
--     CONSTRAINT chk_gateway_token_unique UNIQUE (gateway_provider, gateway_token),
--     CONSTRAINT chk_payment_method_type_values CHECK (method_type IN ('CREDIT_CARD', 'DEBIT_CARD', 'PAYPAL', 'STRIPE', 'OTHER')),
--     CONSTRAINT chk_payment_method_status_values CHECK (status IN ('ACTIVE', 'INACTIVE', 'EXPIRED', 'FAILED', 'REPLACED')),
--     CONSTRAINT chk_payment_method_status_deleted CHECK ((status = 'INACTIVE' AND (deleted_at IS NOT NULL OR is_active = FALSE)) OR (status != 'INACTIVE')),
--     CONSTRAINT chk_card_details CHECK ((method_type = 'CREDIT_CARD' AND card_last_four IS NOT NULL) OR (method_type = 'DEBIT_CARD' AND card_last_four IS NOT NULL) OR (method_type NOT IN ('CREDIT_CARD', 'DEBIT_CARD'))),
--     CONSTRAINT chk_paypal_email CHECK ((method_type = 'PAYPAL' AND paypal_email IS NOT NULL) OR (method_type != 'PAYPAL')),
--     CONSTRAINT fk_payment_method_user FOREIGN KEY (user_id) REFERENCES users.users(id) ON DELETE CASCADE
-- );
--
-- -- Migrate data back
-- INSERT INTO subscriptions.payment_methods SELECT id, user_id, method_type, gateway_provider, gateway_token,
--     card_last_four, card_brand, card_expiry_month, card_expiry_year, paypal_email, billing_name, billing_country,
--     billing_postal_code, billing_email, is_default, is_active, status,
--     purpose::text::subscriptions.payment_method_purpose, verified_at, verification_status, deleted_at, created_at, updated_at
-- FROM payment.payment_methods;
--
-- SELECT setval('subscriptions.payment_methods_id_seq', COALESCE((SELECT MAX(id) FROM subscriptions.payment_methods), 1), true);
--
-- -- Drop FKs from subscriptions pointing to payment
-- ALTER TABLE subscriptions.user_subscriptions DROP CONSTRAINT IF EXISTS fk_user_subscription_payment_method;
-- ALTER TABLE subscriptions.subscription_transactions DROP CONSTRAINT IF EXISTS fk_transaction_payment_method;
--
-- -- Recreate FKs pointing to subscriptions
-- ALTER TABLE subscriptions.user_subscriptions ADD CONSTRAINT fk_user_subscription_payment_method FOREIGN KEY (payment_method_id) REFERENCES subscriptions.payment_methods(id) ON DELETE SET NULL;
-- ALTER TABLE subscriptions.subscription_transactions ADD CONSTRAINT fk_transaction_payment_method FOREIGN KEY (payment_method_id) REFERENCES subscriptions.payment_methods(id) ON DELETE SET NULL;
--
-- -- Drop payment schema objects
-- DROP TRIGGER IF EXISTS update_payment_methods_updated_at ON payment.payment_methods;
-- DROP TRIGGER IF EXISTS ensure_single_default_payment_method_trigger ON payment.payment_methods;
-- DROP FUNCTION IF EXISTS payment.update_payment_methods_updated_at();
-- DROP FUNCTION IF EXISTS payment.ensure_single_default_payment_method();
-- DROP FUNCTION IF EXISTS payment.mark_expired_payment_methods();
-- DROP TABLE IF EXISTS payment.payment_methods CASCADE;
-- DROP TYPE IF EXISTS payment.payment_method_purpose;
--
-- -- Restore subscriptions.update_payment_method_purpose_to_host() to reference subscriptions.payment_methods
-- -- (See V81 for original function body)
--
-- COMMIT;
