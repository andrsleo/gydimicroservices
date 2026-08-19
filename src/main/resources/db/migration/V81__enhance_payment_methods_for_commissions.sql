-- V81__enhance_payment_methods_for_commissions.sql
-- Purpose: Enhance payment infrastructure to support commission processing
--   1. Add 'purpose' field to payment_methods (SUBSCRIPTION, HOST_COMMISSION, BOTH)
--   2. Create stripe_connect_accounts table for affiliate payouts
--   3. Add Stripe transaction IDs to commission tables (already exist from V79)
--   4. Create user_onboarding_status table to track multi-step onboarding
--   5. Add 'country' column to users for Stripe Connect multi-country support
-- Author: Backend AI + Database Architect
-- Date: 2026-02-12
-- Dependencies: V79 (commissions schema), V63 (payment_methods status)

BEGIN;

-- ============================================================================
-- 1. ADD 'purpose' COLUMN TO payment_methods
-- ============================================================================
-- Purpose: Track whether payment method is for subscription payments, host commission charges, or both
-- Decision: Reutilizar payment methods de subscripciones para cobrar comisiones de host (single source of truth)

-- Create enum for payment method purpose
CREATE TYPE subscriptions.payment_method_purpose AS ENUM (
    'SUBSCRIPTION',      -- Only used for subscription auto-billing
    'HOST_COMMISSION',   -- Only used for host commission charges
    'BOTH'               -- Used for both subscription AND host commission charges
);

COMMENT ON TYPE subscriptions.payment_method_purpose IS
'Indicates the purpose of a payment method. SUBSCRIPTION=subscription billing only, HOST_COMMISSION=host commission charges only, BOTH=shared for both purposes. Enables reuse of subscription payment methods for commission processing.';

-- Add purpose column
ALTER TABLE subscriptions.payment_methods
    ADD COLUMN purpose subscriptions.payment_method_purpose NOT NULL DEFAULT 'SUBSCRIPTION';

COMMENT ON COLUMN subscriptions.payment_methods.purpose IS
'Purpose of this payment method. SUBSCRIPTION=subscription auto-billing, HOST_COMMISSION=host commission charges, BOTH=shared for both purposes. Defaults to SUBSCRIPTION for backward compatibility.';

-- Add verification fields (for $1 auth test or similar)
ALTER TABLE subscriptions.payment_methods
    ADD COLUMN verified_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN verification_status VARCHAR(20) DEFAULT 'PENDING' CHECK (verification_status IN ('PENDING', 'VERIFIED', 'FAILED'));

COMMENT ON COLUMN subscriptions.payment_methods.verified_at IS
'Timestamp when payment method was verified (e.g., via $1 authorization test). NULL if not yet verified.';

COMMENT ON COLUMN subscriptions.payment_methods.verification_status IS
'Verification status of payment method. PENDING=awaiting verification, VERIFIED=successfully verified, FAILED=verification failed.';

-- Create index for queries by purpose
CREATE INDEX idx_payment_methods_purpose
    ON subscriptions.payment_methods(user_id, purpose, status)
    WHERE deleted_at IS NULL;

COMMENT ON INDEX subscriptions.idx_payment_methods_purpose IS
'Index for finding payment methods by user and purpose (SUBSCRIPTION, HOST_COMMISSION, BOTH). Used when selecting payment method for commission charges.';


-- ============================================================================
-- 2. CREATE stripe_connect_accounts TABLE (AFFILIATE PAYOUTS)
-- ============================================================================
-- Purpose: Track Stripe Connect accounts for affiliate payouts
-- Decision: Use Stripe Connect Standard/Express/Custom accounts per country
-- See: https://stripe.com/docs/connect/accounts

CREATE TABLE commissions.stripe_connect_accounts (
    -- Primary key
    id BIGSERIAL PRIMARY KEY,

    -- Foreign key
    user_id BIGINT NOT NULL UNIQUE,

    -- Stripe Connect Account details
    stripe_account_id VARCHAR(255) NOT NULL UNIQUE,  -- acct_xxxxx from Stripe
    account_type VARCHAR(20) NOT NULL DEFAULT 'standard',  -- standard, express, custom
    country VARCHAR(2) NOT NULL,  -- ISO 3166-1 alpha-2 (US, CA, GB, MX, etc.)

    -- Onboarding status
    onboarding_completed BOOLEAN NOT NULL DEFAULT FALSE,  -- account.details_submitted from Stripe
    payouts_enabled BOOLEAN NOT NULL DEFAULT FALSE,       -- account.payouts_enabled from Stripe
    charges_enabled BOOLEAN NOT NULL DEFAULT FALSE,       -- account.charges_enabled from Stripe (usually false for standard)

    -- Onboarding metadata
    onboarding_link_created_at TIMESTAMP WITH TIME ZONE,
    onboarding_completed_at TIMESTAMP WITH TIME ZONE,

    -- Payout information (from Stripe Account object)
    default_currency VARCHAR(3) DEFAULT 'USD',
    payout_schedule_interval VARCHAR(20),  -- daily, weekly, monthly, manual
    payout_schedule_delay_days INT DEFAULT 7,  -- Stripe default = 7 days

    -- Risk management
    disabled_reason TEXT,  -- If payouts are disabled by Stripe (fraud, ToS violation)
    requirements_due_by DATE,  -- Date when Stripe requires updated information

    -- Audit timestamps
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Foreign key constraint
    CONSTRAINT fk_stripe_connect_account_user
        FOREIGN KEY (user_id)
        REFERENCES users.users(id)
        ON DELETE CASCADE,  -- If user deleted, cascade delete connect account

    -- Business rules
    CONSTRAINT chk_stripe_connect_account_type
        CHECK (account_type IN ('standard', 'express', 'custom')),

    CONSTRAINT chk_stripe_connect_country
        CHECK (country ~ '^[A-Z]{2}$'),  -- Must be 2-letter uppercase code

    CONSTRAINT chk_stripe_connect_currency
        CHECK (default_currency IN ('USD', 'EUR', 'GBP', 'CAD', 'MXN', 'AUD')),

    CONSTRAINT chk_stripe_connect_payout_schedule
        CHECK (payout_schedule_interval IN ('daily', 'weekly', 'monthly', 'manual'))
);

-- Indexes
CREATE INDEX idx_stripe_connect_accounts_user
    ON commissions.stripe_connect_accounts(user_id);

CREATE INDEX idx_stripe_connect_accounts_stripe_account
    ON commissions.stripe_connect_accounts(stripe_account_id);

CREATE INDEX idx_stripe_connect_accounts_onboarding_status
    ON commissions.stripe_connect_accounts(onboarding_completed, payouts_enabled)
    WHERE onboarding_completed = FALSE OR payouts_enabled = FALSE;

COMMENT ON INDEX commissions.idx_stripe_connect_accounts_onboarding_status IS
'Index for finding Connect accounts that need onboarding completion or payout enablement.';

-- Table comment
COMMENT ON TABLE commissions.stripe_connect_accounts IS
'Stripe Connect accounts for affiliate payouts. One account per affiliate user. Supports multi-country payouts using Stripe Connect Standard, Express, or Custom accounts depending on country.';

-- Trigger for updated_at
CREATE TRIGGER tr_stripe_connect_accounts_updated_at
    BEFORE UPDATE ON commissions.stripe_connect_accounts
    FOR EACH ROW
    EXECUTE FUNCTION commissions.update_commission_updated_at();


-- ============================================================================
-- 3. VERIFY commissions.host_commission HAS STRIPE FIELDS (FROM V79)
-- ============================================================================
-- These fields were already added in V79, this is just verification

DO $$
DECLARE
    has_stripe_payment_intent BOOLEAN;
    has_stripe_charge BOOLEAN;
    has_charged_at BOOLEAN;
BEGIN
    -- Check stripe_payment_intent_id column exists
    SELECT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'commissions'
          AND table_name = 'host_commission'
          AND column_name = 'stripe_payment_intent_id'
    ) INTO has_stripe_payment_intent;

    -- Check stripe_charge_id column exists
    SELECT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'commissions'
          AND table_name = 'host_commission'
          AND column_name = 'stripe_charge_id'
    ) INTO has_stripe_charge;

    -- Check charged_at column exists
    SELECT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'commissions'
          AND table_name = 'host_commission'
          AND column_name = 'charged_at'
    ) INTO has_charged_at;

    IF NOT has_stripe_payment_intent THEN
        RAISE EXCEPTION 'commissions.host_commission missing stripe_payment_intent_id column (should exist from V79)';
    END IF;

    IF NOT has_stripe_charge THEN
        RAISE EXCEPTION 'commissions.host_commission missing stripe_charge_id column (should exist from V79)';
    END IF;

    IF NOT has_charged_at THEN
        RAISE EXCEPTION 'commissions.host_commission missing charged_at column (should exist from V79)';
    END IF;

    RAISE NOTICE '✓ commissions.host_commission already has Stripe payment fields (from V79)';
END $$;


-- ============================================================================
-- 4. VERIFY commissions.affiliate_commission HAS STRIPE FIELDS (FROM V79)
-- ============================================================================
-- These fields were already added in V79, this is just verification

DO $$
DECLARE
    has_stripe_transfer BOOLEAN;
    has_stripe_payout BOOLEAN;
    has_paid_at BOOLEAN;
    has_scheduled_payment_date BOOLEAN;
BEGIN
    -- Check stripe_transfer_id column exists
    SELECT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'commissions'
          AND table_name = 'affiliate_commission'
          AND column_name = 'stripe_transfer_id'
    ) INTO has_stripe_transfer;

    -- Check stripe_payout_id column exists
    SELECT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'commissions'
          AND table_name = 'affiliate_commission'
          AND column_name = 'stripe_payout_id'
    ) INTO has_stripe_payout;

    -- Check paid_at column exists
    SELECT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'commissions'
          AND table_name = 'affiliate_commission'
          AND column_name = 'paid_at'
    ) INTO has_paid_at;

    -- Check scheduled_payment_date column exists
    SELECT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'commissions'
          AND table_name = 'affiliate_commission'
          AND column_name = 'scheduled_payment_date'
    ) INTO has_scheduled_payment_date;

    IF NOT has_stripe_transfer THEN
        RAISE EXCEPTION 'commissions.affiliate_commission missing stripe_transfer_id column (should exist from V79)';
    END IF;

    IF NOT has_stripe_payout THEN
        RAISE EXCEPTION 'commissions.affiliate_commission missing stripe_payout_id column (should exist from V79)';
    END IF;

    IF NOT has_paid_at THEN
        RAISE EXCEPTION 'commissions.affiliate_commission missing paid_at column (should exist from V79)';
    END IF;

    IF NOT has_scheduled_payment_date THEN
        RAISE EXCEPTION 'commissions.affiliate_commission missing scheduled_payment_date column (should exist from V79)';
    END IF;

    RAISE NOTICE '✓ commissions.affiliate_commission already has Stripe payout fields (from V79)';
END $$;


-- ============================================================================
-- 5. CREATE user_onboarding_status TABLE
-- ============================================================================
-- Purpose: Track onboarding progress for dual-role users (HOST + AFFILIATE)
-- Use case: Dashboard shows checklist of required steps

CREATE TABLE users.user_onboarding_status (
    -- Primary key
    id BIGSERIAL PRIMARY KEY,

    -- Foreign key
    user_id BIGINT NOT NULL UNIQUE,

    -- HOST onboarding (required to publish properties)
    host_payment_method_added BOOLEAN NOT NULL DEFAULT FALSE,
    host_payment_method_verified BOOLEAN NOT NULL DEFAULT FALSE,
    host_payment_method_added_at TIMESTAMP WITH TIME ZONE,

    -- AFFILIATE onboarding (required to receive payouts)
    affiliate_connect_completed BOOLEAN NOT NULL DEFAULT FALSE,
    affiliate_connect_payouts_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    affiliate_connect_completed_at TIMESTAMP WITH TIME ZONE,

    -- Audit timestamps
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Foreign key constraint
    CONSTRAINT fk_user_onboarding_status_user
        FOREIGN KEY (user_id)
        REFERENCES users.users(id)
        ON DELETE CASCADE
);

-- Index
CREATE INDEX idx_user_onboarding_status_user
    ON users.user_onboarding_status(user_id);

CREATE INDEX idx_user_onboarding_incomplete
    ON users.user_onboarding_status(
        host_payment_method_added,
        affiliate_connect_completed
    )
    WHERE host_payment_method_added = FALSE OR affiliate_connect_completed = FALSE;

COMMENT ON INDEX users.idx_user_onboarding_incomplete IS
'Index for finding users with incomplete onboarding (missing payment method or Connect account).';

-- Table comment
COMMENT ON TABLE users.user_onboarding_status IS
'Tracks onboarding progress for users with multiple roles (HOST + AFFILIATE). HOST needs payment method for commission charges. AFFILIATE needs Stripe Connect account for payouts.';

-- Trigger for updated_at
CREATE OR REPLACE FUNCTION users.update_user_onboarding_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER tr_user_onboarding_status_updated_at
    BEFORE UPDATE ON users.user_onboarding_status
    FOR EACH ROW
    EXECUTE FUNCTION users.update_user_onboarding_updated_at();


-- ============================================================================
-- 6. ADD 'country' COLUMN TO users.users (MULTI-COUNTRY STRIPE CONNECT)
-- ============================================================================
-- Purpose: Store user's country for Stripe Connect account creation
-- Decision: Support all countries from day 1 (Standard/Express/Custom accounts)

ALTER TABLE users.users
    ADD COLUMN country VARCHAR(2),  -- ISO 3166-1 alpha-2 code
    ADD COLUMN detected_country VARCHAR(2);  -- Auto-detected from IP/browser (for pre-fill)

COMMENT ON COLUMN users.users.country IS
'User country (ISO 3166-1 alpha-2). Required for Stripe Connect account creation. Determines account type (Standard for US/CA/UK, Express for EU, Custom for others).';

COMMENT ON COLUMN users.users.detected_country IS
'Auto-detected country from IP or browser locale. Used to pre-fill country field in onboarding if user has not set it manually.';

-- Create index for country queries (analytics, compliance reporting)
CREATE INDEX idx_users_country
    ON users.users(country)
    WHERE country IS NOT NULL;


-- ============================================================================
-- 7. HELPER FUNCTIONS
-- ============================================================================

-- Function to update payment_method purpose when user becomes HOST
CREATE OR REPLACE FUNCTION subscriptions.update_payment_method_purpose_to_host(
    p_user_id BIGINT
)
RETURNS VOID AS $$
BEGIN
    -- Update default payment method to BOTH (SUBSCRIPTION + HOST_COMMISSION)
    UPDATE subscriptions.payment_methods
    SET purpose = 'BOTH',
        updated_at = CURRENT_TIMESTAMP
    WHERE user_id = p_user_id
      AND is_default = TRUE
      AND status = 'ACTIVE'
      AND deleted_at IS NULL
      AND purpose = 'SUBSCRIPTION';  -- Only update if currently SUBSCRIPTION-only

    -- Update onboarding status
    INSERT INTO users.user_onboarding_status (
        user_id,
        host_payment_method_added,
        host_payment_method_verified,
        host_payment_method_added_at
    ) VALUES (
        p_user_id,
        TRUE,
        TRUE,  -- Assume verified since it was already used for subscription
        CURRENT_TIMESTAMP
    )
    ON CONFLICT (user_id) DO UPDATE
    SET host_payment_method_added = TRUE,
        host_payment_method_verified = TRUE,
        host_payment_method_added_at = CURRENT_TIMESTAMP,
        updated_at = CURRENT_TIMESTAMP;

    RAISE NOTICE 'Updated payment method purpose to BOTH for user %', p_user_id;
END;
$$ LANGUAGE plpgsql;

COMMENT ON FUNCTION subscriptions.update_payment_method_purpose_to_host IS
'Updates default payment method purpose from SUBSCRIPTION to BOTH when user publishes first property (becomes HOST). Also updates user_onboarding_status.';


-- Function to mark affiliate Connect account as complete
CREATE OR REPLACE FUNCTION commissions.mark_affiliate_onboarding_complete(
    p_user_id BIGINT,
    p_payouts_enabled BOOLEAN DEFAULT TRUE
)
RETURNS VOID AS $$
BEGIN
    -- Update onboarding status
    INSERT INTO users.user_onboarding_status (
        user_id,
        affiliate_connect_completed,
        affiliate_connect_payouts_enabled,
        affiliate_connect_completed_at
    ) VALUES (
        p_user_id,
        TRUE,
        p_payouts_enabled,
        CURRENT_TIMESTAMP
    )
    ON CONFLICT (user_id) DO UPDATE
    SET affiliate_connect_completed = TRUE,
        affiliate_connect_payouts_enabled = p_payouts_enabled,
        affiliate_connect_completed_at = CURRENT_TIMESTAMP,
        updated_at = CURRENT_TIMESTAMP;

    RAISE NOTICE 'Marked affiliate onboarding complete for user %', p_user_id;
END;
$$ LANGUAGE plpgsql;

COMMENT ON FUNCTION commissions.mark_affiliate_onboarding_complete IS
'Marks affiliate onboarding as complete in user_onboarding_status. Called when Stripe webhook account.updated confirms onboarding_completed=true.';


-- Function to get user onboarding progress (for dashboard)
CREATE OR REPLACE FUNCTION users.get_onboarding_progress(
    p_user_id BIGINT
)
RETURNS TABLE (
    user_id BIGINT,
    has_subscription BOOLEAN,
    host_payment_ready BOOLEAN,
    affiliate_payout_ready BOOLEAN,
    onboarding_complete BOOLEAN
) AS $$
BEGIN
    RETURN QUERY
    SELECT
        u.id AS user_id,
        EXISTS (
            SELECT 1 FROM subscriptions.user_subscriptions us
            WHERE us.user_id = u.id
              AND us.status = 'ACTIVE'
        ) AS has_subscription,
        COALESCE(uos.host_payment_method_added AND uos.host_payment_method_verified, FALSE) AS host_payment_ready,
        COALESCE(uos.affiliate_connect_completed AND uos.affiliate_connect_payouts_enabled, FALSE) AS affiliate_payout_ready,
        (
            COALESCE(uos.host_payment_method_added AND uos.host_payment_method_verified, FALSE) AND
            COALESCE(uos.affiliate_connect_completed AND uos.affiliate_connect_payouts_enabled, FALSE)
        ) AS onboarding_complete
    FROM users.users u
    LEFT JOIN users.user_onboarding_status uos ON u.id = uos.user_id
    WHERE u.id = p_user_id;
END;
$$ LANGUAGE plpgsql;

COMMENT ON FUNCTION users.get_onboarding_progress IS
'Returns onboarding progress for a user. Used in dashboard to show checklist of required steps.';


-- ============================================================================
-- 8. DATA MIGRATION (EXISTING USERS WITH SUBSCRIPTIONS)
-- ============================================================================
-- Any user with an active subscription and PRO/ELITE plan should have their
-- payment_methods.purpose set to BOTH (in case they publish a property)

DO $$
DECLARE
    updated_count INTEGER;
BEGIN
    WITH updated AS (
        UPDATE subscriptions.payment_methods pm
        SET purpose = 'BOTH',
            updated_at = CURRENT_TIMESTAMP
        WHERE pm.is_default = TRUE
          AND pm.status = 'ACTIVE'
          AND pm.deleted_at IS NULL
          AND pm.purpose = 'SUBSCRIPTION'
          AND EXISTS (
              SELECT 1
              FROM subscriptions.user_subscriptions us
              JOIN subscriptions.subscription_plans sp ON us.plan_id = sp.id
              WHERE us.user_id = pm.user_id
                AND us.status = 'ACTIVE'
                AND sp.plan_code IN ('PRO', 'ELITE')  -- Only paid users
          )
        RETURNING id
    )
    SELECT COUNT(*)::INTEGER INTO updated_count FROM updated;

    RAISE NOTICE 'Updated % payment methods from SUBSCRIPTION to BOTH for existing PRO/ELITE users', updated_count;
END $$;

-- Initialize onboarding status for existing users with active subscriptions
INSERT INTO users.user_onboarding_status (
    user_id,
    host_payment_method_added,
    host_payment_method_verified,
    host_payment_method_added_at
)
SELECT DISTINCT
    pm.user_id,
    TRUE,  -- Has payment method
    TRUE,  -- Assume verified (already used for subscription)
    pm.created_at
FROM subscriptions.payment_methods pm
JOIN subscriptions.user_subscriptions us ON pm.user_id = us.user_id
WHERE pm.is_default = TRUE
  AND pm.status = 'ACTIVE'
  AND pm.deleted_at IS NULL
  AND us.status = 'ACTIVE'
ON CONFLICT (user_id) DO NOTHING;  -- Don't overwrite if already exists


-- ============================================================================
-- 9. VERIFICATION
-- ============================================================================

DO $$
DECLARE
    payment_methods_purpose_count BIGINT;
    connect_accounts_count BIGINT;
    onboarding_status_count BIGINT;
    users_country_count BIGINT;
BEGIN
    -- Verify payment_methods has purpose column
    SELECT COUNT(*) INTO payment_methods_purpose_count
    FROM information_schema.columns
    WHERE table_schema = 'subscriptions'
      AND table_name = 'payment_methods'
      AND column_name = 'purpose';

    IF payment_methods_purpose_count = 0 THEN
        RAISE EXCEPTION 'payment_methods.purpose column was not created';
    END IF;

    -- Verify stripe_connect_accounts table exists
    SELECT COUNT(*) INTO connect_accounts_count
    FROM information_schema.tables
    WHERE table_schema = 'commissions'
      AND table_name = 'stripe_connect_accounts';

    IF connect_accounts_count = 0 THEN
        RAISE EXCEPTION 'commissions.stripe_connect_accounts table was not created';
    END IF;

    -- Verify user_onboarding_status table exists
    SELECT COUNT(*) INTO onboarding_status_count
    FROM information_schema.tables
    WHERE table_schema = 'users'
      AND table_name = 'user_onboarding_status';

    IF onboarding_status_count = 0 THEN
        RAISE EXCEPTION 'users.user_onboarding_status table was not created';
    END IF;

    -- Verify users has country column
    SELECT COUNT(*) INTO users_country_count
    FROM information_schema.columns
    WHERE table_schema = 'users'
      AND table_name = 'users'
      AND column_name = 'country';

    IF users_country_count = 0 THEN
        RAISE EXCEPTION 'users.country column was not created';
    END IF;

    RAISE NOTICE '';
    RAISE NOTICE '✅ V81 Migration successful:';
    RAISE NOTICE '   ✓ payment_methods.purpose added (SUBSCRIPTION, HOST_COMMISSION, BOTH)';
    RAISE NOTICE '   ✓ payment_methods.verified_at and verification_status added';
    RAISE NOTICE '   ✓ commissions.stripe_connect_accounts table created';
    RAISE NOTICE '   ✓ users.user_onboarding_status table created';
    RAISE NOTICE '   ✓ users.country and detected_country columns added';
    RAISE NOTICE '   ✓ Helper functions created for onboarding workflows';
    RAISE NOTICE '   ✓ Existing PRO/ELITE users migrated to purpose=BOTH';
    RAISE NOTICE '';
END $$;

COMMIT;

-- ============================================================================
-- USAGE EXAMPLES
-- ============================================================================

-- Example 1: Update payment method purpose when user publishes first property
-- SELECT subscriptions.update_payment_method_purpose_to_host(123);

-- Example 2: Mark affiliate onboarding complete (called from webhook handler)
-- SELECT commissions.mark_affiliate_onboarding_complete(123, TRUE);

-- Example 3: Get user onboarding progress
-- SELECT * FROM users.get_onboarding_progress(123);

-- Example 4: Find users with incomplete onboarding
-- SELECT u.id, u.email, uos.*
-- FROM users.users u
-- JOIN users.user_onboarding_status uos ON u.id = uos.user_id
-- WHERE uos.host_payment_method_added = FALSE
--    OR uos.affiliate_connect_completed = FALSE;

-- Example 5: Get affiliate Connect accounts pending onboarding
-- SELECT sca.*, u.email
-- FROM commissions.stripe_connect_accounts sca
-- JOIN users.users u ON sca.user_id = u.id
-- WHERE sca.onboarding_completed = FALSE
--    OR sca.payouts_enabled = FALSE;

-- ============================================================================
-- ROLLBACK INSTRUCTIONS (IF NEEDED)
-- ============================================================================

-- To rollback this migration:
--
-- BEGIN;
--
-- DROP FUNCTION IF EXISTS users.get_onboarding_progress(BIGINT);
-- DROP FUNCTION IF EXISTS commissions.mark_affiliate_onboarding_complete(BIGINT, BOOLEAN);
-- DROP FUNCTION IF EXISTS subscriptions.update_payment_method_purpose_to_host(BIGINT);
-- DROP FUNCTION IF EXISTS users.update_user_onboarding_updated_at() CASCADE;
--
-- ALTER TABLE users.users DROP COLUMN IF EXISTS country;
-- ALTER TABLE users.users DROP COLUMN IF EXISTS detected_country;
-- DROP INDEX IF EXISTS users.idx_users_country;
--
-- DROP TABLE IF EXISTS users.user_onboarding_status CASCADE;
--
-- DROP TABLE IF EXISTS commissions.stripe_connect_accounts CASCADE;
--
-- ALTER TABLE subscriptions.payment_methods DROP COLUMN IF EXISTS verification_status;
-- ALTER TABLE subscriptions.payment_methods DROP COLUMN IF EXISTS verified_at;
-- ALTER TABLE subscriptions.payment_methods DROP COLUMN IF EXISTS purpose;
-- DROP INDEX IF EXISTS subscriptions.idx_payment_methods_purpose;
-- DROP TYPE IF EXISTS subscriptions.payment_method_purpose;
--
-- COMMIT;
