-- V52__create_subscription_plans_and_tables.sql
-- Purpose: Create subscription plans catalog and related tables
-- Author: Database Architect
-- Date: 2025-12-09
-- Dependencies: V51__create_subscriptions_schema.sql

BEGIN;

-- ============================================================================
-- SUBSCRIPTION PLANS TABLE (CATALOG)
-- ============================================================================
-- Catalog of available subscription plans (FREE, PRO, ELITE)
-- This is a relatively static table - plans rarely change
-- Changes to plan features should be versioned (create new plan, deprecate old)

CREATE TABLE subscriptions.subscription_plans (
    -- Primary key
    id BIGSERIAL PRIMARY KEY,

    -- Plan identification
    plan_code VARCHAR(20) NOT NULL UNIQUE,  -- 'FREE', 'PRO', 'ELITE'
    plan_name VARCHAR(100) NOT NULL,        -- 'Free Plan', 'Pro Plan', 'Elite Plan'
    plan_description TEXT,                  -- Marketing description

    -- Pricing
    monthly_price DECIMAL(10,2) NOT NULL CHECK (monthly_price >= 0),  -- USD
    currency VARCHAR(3) NOT NULL DEFAULT 'USD',

    -- Commission settings
    commission_rate DECIMAL(5,4) NOT NULL CHECK (commission_rate >= 0 AND commission_rate <= 1),
    -- Stored as decimal (0.02 = 2%, 0.05 = 5%, 0.10 = 10%)

    -- Feature limits
    referral_limit_per_month INT NOT NULL CHECK (referral_limit_per_month >= 0),
    -- -1 or NULL = unlimited, otherwise specific limit
    property_publish_limit INT NOT NULL CHECK (property_publish_limit >= 0),
    -- -1 or NULL = unlimited, otherwise specific limit

    -- Plan status
    is_active BOOLEAN NOT NULL DEFAULT TRUE,  -- Can users subscribe to this plan?
    is_featured BOOLEAN NOT NULL DEFAULT FALSE,  -- Show as featured on pricing page
    display_order INT NOT NULL DEFAULT 0,     -- Sort order for display

    -- Audit timestamps
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Business rules
    CONSTRAINT chk_plan_code_upper CHECK (plan_code = UPPER(plan_code)),
    CONSTRAINT chk_free_plan_price CHECK (
        (plan_code = 'FREE' AND monthly_price = 0) OR
        (plan_code != 'FREE' AND monthly_price > 0)
    )
);

-- ============================================================================
-- USER SUBSCRIPTIONS TABLE
-- ============================================================================
-- Tracks active subscription for each user (1:1 relationship with users)
-- Users MUST have exactly one active subscription at all times
-- New users get FREE plan by default (handled by application or trigger)

CREATE TABLE subscriptions.user_subscriptions (
    -- Primary key
    id BIGSERIAL PRIMARY KEY,

    -- Foreign keys
    user_id BIGINT NOT NULL UNIQUE,  -- One subscription per user
    plan_id BIGINT NOT NULL,         -- Current plan

    -- Subscription period
    started_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP WITH TIME ZONE,  -- NULL for FREE plan (never expires)

    -- Status
    status subscriptions.subscription_status NOT NULL DEFAULT 'ACTIVE',

    -- Auto-renewal settings
    auto_renew BOOLEAN NOT NULL DEFAULT FALSE,  -- Auto-renew on expiration?
    payment_method_id BIGINT,  -- Default payment method for renewal (can be NULL)

    -- Cancellation tracking
    canceled_at TIMESTAMP WITH TIME ZONE,
    cancellation_reason TEXT,

    -- Audit timestamps
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Foreign key constraints
    CONSTRAINT fk_user_subscription_user FOREIGN KEY (user_id)
        REFERENCES users.users(id) ON DELETE CASCADE,
    CONSTRAINT fk_user_subscription_plan FOREIGN KEY (plan_id)
        REFERENCES subscriptions.subscription_plans(id) ON DELETE RESTRICT,
    -- Note: FK to payment_methods will be added after payment_methods table is created

    -- Business rules
    -- NOTE: Cannot use subquery in CHECK constraint
    -- Plan-specific rules will be enforced by application logic or triggers
    CONSTRAINT chk_canceled_status CHECK (
        (status = 'CANCELED' AND canceled_at IS NOT NULL) OR
        (status != 'CANCELED' AND canceled_at IS NULL)
    ),
    CONSTRAINT chk_auto_renew_payment_method CHECK (
        (auto_renew = TRUE AND payment_method_id IS NOT NULL) OR
        (auto_renew = FALSE)
    )
);

-- ============================================================================
-- PAYMENT METHODS TABLE
-- ============================================================================
-- Stores tokenized payment methods for users
-- NEVER store raw card numbers - only gateway tokens
-- Supports multiple payment methods per user

CREATE TABLE subscriptions.payment_methods (
    -- Primary key
    id BIGSERIAL PRIMARY KEY,

    -- Foreign keys
    user_id BIGINT NOT NULL,

    -- Payment method details
    method_type subscriptions.payment_method_type NOT NULL DEFAULT 'CREDIT_CARD',

    -- Tokenized payment data (from payment gateway)
    gateway_provider VARCHAR(50) NOT NULL,  -- 'stripe', 'paypal', etc.
    gateway_token VARCHAR(255) NOT NULL,    -- Token from gateway (e.g., Stripe payment method ID)

    -- Display information (for user identification, NEVER full card number)
    card_last_four CHAR(4),                 -- Last 4 digits (for display only)
    card_brand VARCHAR(50),                 -- 'Visa', 'Mastercard', 'Amex', etc.
    card_expiry_month INT CHECK (card_expiry_month >= 1 AND card_expiry_month <= 12),
    card_expiry_year INT CHECK (card_expiry_year >= 2025),

    -- PayPal details (if applicable)
    paypal_email VARCHAR(255),

    -- Billing address
    billing_name VARCHAR(255),
    billing_country VARCHAR(2),  -- ISO 3166-1 alpha-2 code
    billing_postal_code VARCHAR(20),

    -- Status
    is_default BOOLEAN NOT NULL DEFAULT FALSE,  -- Default payment method for this user
    is_active BOOLEAN NOT NULL DEFAULT TRUE,    -- Can be used for payments?

    -- Soft delete (never physically delete payment methods for audit)
    deleted_at TIMESTAMP WITH TIME ZONE,

    -- Audit timestamps
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Foreign key constraints
    CONSTRAINT fk_payment_method_user FOREIGN KEY (user_id)
        REFERENCES users.users(id) ON DELETE CASCADE,

    -- Business rules
    CONSTRAINT chk_gateway_token_unique UNIQUE (gateway_provider, gateway_token),
    CONSTRAINT chk_card_details CHECK (
        (method_type = 'CREDIT_CARD' AND card_last_four IS NOT NULL) OR
        (method_type = 'DEBIT_CARD' AND card_last_four IS NOT NULL) OR
        (method_type NOT IN ('CREDIT_CARD', 'DEBIT_CARD'))
    ),
    CONSTRAINT chk_paypal_email CHECK (
        (method_type = 'PAYPAL' AND paypal_email IS NOT NULL) OR
        (method_type != 'PAYPAL')
    )
);

-- ============================================================================
-- SUBSCRIPTION TRANSACTIONS TABLE
-- ============================================================================
-- Immutable audit log of all subscription-related transactions
-- Tracks plan changes, payments, renewals, cancellations
-- NEVER delete records - this is financial audit trail

CREATE TABLE subscriptions.subscription_transactions (
    -- Primary key
    id BIGSERIAL PRIMARY KEY,

    -- Foreign keys
    user_subscription_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,  -- Denormalized for fast queries
    from_plan_id BIGINT,      -- Previous plan (NULL for initial subscription)
    to_plan_id BIGINT NOT NULL,  -- New plan
    payment_method_id BIGINT,  -- Payment method used (NULL for FREE plan)

    -- Transaction details
    transaction_type subscriptions.transaction_type NOT NULL,
    transaction_status subscriptions.transaction_status NOT NULL DEFAULT 'PENDING',

    -- Financial data
    amount DECIMAL(10,2) NOT NULL DEFAULT 0 CHECK (amount >= 0),  -- Amount charged (0 for FREE)
    currency VARCHAR(3) NOT NULL DEFAULT 'USD',

    -- Payment gateway details
    gateway_provider VARCHAR(50),  -- 'stripe', 'paypal', etc.
    gateway_transaction_id VARCHAR(255),  -- Transaction ID from gateway
    gateway_response JSONB,  -- Full response from gateway (for debugging)

    -- Subscription period
    period_start TIMESTAMP WITH TIME ZONE NOT NULL,
    period_end TIMESTAMP WITH TIME ZONE,  -- NULL for FREE plan

    -- Failure tracking
    failure_reason TEXT,  -- Why payment failed (if applicable)
    retry_count INT NOT NULL DEFAULT 0,  -- Number of retry attempts

    -- Metadata
    metadata JSONB,  -- Additional transaction data (promo codes, discounts, etc.)

    -- Audit timestamps
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP WITH TIME ZONE,  -- When transaction completed

    -- Foreign key constraints
    CONSTRAINT fk_transaction_user_subscription FOREIGN KEY (user_subscription_id)
        REFERENCES subscriptions.user_subscriptions(id) ON DELETE CASCADE,
    CONSTRAINT fk_transaction_user FOREIGN KEY (user_id)
        REFERENCES users.users(id) ON DELETE CASCADE,
    CONSTRAINT fk_transaction_from_plan FOREIGN KEY (from_plan_id)
        REFERENCES subscriptions.subscription_plans(id) ON DELETE RESTRICT,
    CONSTRAINT fk_transaction_to_plan FOREIGN KEY (to_plan_id)
        REFERENCES subscriptions.subscription_plans(id) ON DELETE RESTRICT,
    CONSTRAINT fk_transaction_payment_method FOREIGN KEY (payment_method_id)
        REFERENCES subscriptions.payment_methods(id) ON DELETE SET NULL,

    -- Business rules
    CONSTRAINT chk_completed_timestamp CHECK (
        (transaction_status = 'COMPLETED' AND completed_at IS NOT NULL) OR
        (transaction_status != 'COMPLETED')
    ),
    -- NOTE: Cannot use subquery in CHECK constraint
    -- Free plan zero amount rule will be enforced by application logic
    CONSTRAINT chk_paid_plan_payment_method CHECK (
        (amount > 0 AND payment_method_id IS NOT NULL) OR
        (amount = 0)
    )
);

-- ============================================================================
-- INDEXES FOR PERFORMANCE
-- ============================================================================

-- Subscription plans indexes
CREATE INDEX idx_subscription_plans_active
ON subscriptions.subscription_plans(is_active, display_order)
WHERE is_active = TRUE;

CREATE INDEX idx_subscription_plans_code
ON subscriptions.subscription_plans(plan_code);

-- User subscriptions indexes
CREATE INDEX idx_user_subscriptions_user
ON subscriptions.user_subscriptions(user_id, status);

CREATE INDEX idx_user_subscriptions_expiring
ON subscriptions.user_subscriptions(expires_at, status, auto_renew)
WHERE status = 'ACTIVE' AND expires_at IS NOT NULL;

CREATE INDEX idx_user_subscriptions_plan
ON subscriptions.user_subscriptions(plan_id, status);

-- Payment methods indexes
CREATE INDEX idx_payment_methods_user
ON subscriptions.payment_methods(user_id, is_active, deleted_at);

CREATE INDEX idx_payment_methods_default
ON subscriptions.payment_methods(user_id, is_default)
WHERE is_default = TRUE AND deleted_at IS NULL;

CREATE INDEX idx_payment_methods_expiring
ON subscriptions.payment_methods(card_expiry_year, card_expiry_month)
WHERE deleted_at IS NULL AND method_type IN ('CREDIT_CARD', 'DEBIT_CARD');

-- Subscription transactions indexes
CREATE INDEX idx_subscription_transactions_user
ON subscriptions.subscription_transactions(user_id, created_at DESC);

CREATE INDEX idx_subscription_transactions_subscription
ON subscriptions.subscription_transactions(user_subscription_id, created_at DESC);

CREATE INDEX idx_subscription_transactions_status
ON subscriptions.subscription_transactions(transaction_status, created_at)
WHERE transaction_status = 'PENDING';

CREATE INDEX idx_subscription_transactions_gateway
ON subscriptions.subscription_transactions(gateway_provider, gateway_transaction_id)
WHERE gateway_transaction_id IS NOT NULL;

CREATE INDEX idx_subscription_transactions_type
ON subscriptions.subscription_transactions(transaction_type, created_at DESC);

-- ============================================================================
-- TRIGGERS FOR UPDATED_AT
-- ============================================================================

CREATE OR REPLACE FUNCTION subscriptions.update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER update_subscription_plans_updated_at
BEFORE UPDATE ON subscriptions.subscription_plans
FOR EACH ROW EXECUTE FUNCTION subscriptions.update_updated_at_column();

CREATE TRIGGER update_user_subscriptions_updated_at
BEFORE UPDATE ON subscriptions.user_subscriptions
FOR EACH ROW EXECUTE FUNCTION subscriptions.update_updated_at_column();

CREATE TRIGGER update_payment_methods_updated_at
BEFORE UPDATE ON subscriptions.payment_methods
FOR EACH ROW EXECUTE FUNCTION subscriptions.update_updated_at_column();

-- ============================================================================
-- TRIGGER: ENSURE ONLY ONE DEFAULT PAYMENT METHOD PER USER
-- ============================================================================
-- When a payment method is set as default, unset all other default methods for that user

CREATE OR REPLACE FUNCTION subscriptions.ensure_single_default_payment_method()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.is_default = TRUE THEN
        UPDATE subscriptions.payment_methods
        SET is_default = FALSE
        WHERE user_id = NEW.user_id
          AND id != NEW.id
          AND is_default = TRUE;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER ensure_single_default_payment_method_trigger
BEFORE INSERT OR UPDATE ON subscriptions.payment_methods
FOR EACH ROW EXECUTE FUNCTION subscriptions.ensure_single_default_payment_method();

-- ============================================================================
-- HELPER FUNCTIONS
-- ============================================================================

-- Function to check if subscription is expired
CREATE OR REPLACE FUNCTION subscriptions.is_subscription_expired(
    p_subscription_id BIGINT
) RETURNS BOOLEAN AS $$
DECLARE
    v_expires_at TIMESTAMP WITH TIME ZONE;
BEGIN
    SELECT expires_at INTO v_expires_at
    FROM subscriptions.user_subscriptions
    WHERE id = p_subscription_id;

    IF v_expires_at IS NULL THEN
        -- FREE plan never expires
        RETURN FALSE;
    END IF;

    RETURN v_expires_at < CURRENT_TIMESTAMP;
END;
$$ LANGUAGE plpgsql;

-- Function to auto-expire subscriptions (run daily via cron)
CREATE OR REPLACE FUNCTION subscriptions.auto_expire_subscriptions()
RETURNS TABLE(expired_count INTEGER) AS $$
DECLARE
    updated_rows INTEGER;
BEGIN
    WITH updated AS (
        UPDATE subscriptions.user_subscriptions
        SET status = 'EXPIRED',
            updated_at = CURRENT_TIMESTAMP
        WHERE status = 'ACTIVE'
          AND expires_at IS NOT NULL
          AND expires_at < CURRENT_TIMESTAMP
        RETURNING id
    )
    SELECT COUNT(*)::INTEGER INTO updated_rows FROM updated;

    RAISE NOTICE 'Auto-expired % subscriptions', updated_rows;
    RETURN QUERY SELECT updated_rows;
END;
$$ LANGUAGE plpgsql;

-- Function to get user's current plan details
CREATE OR REPLACE FUNCTION subscriptions.get_user_plan(
    p_user_id BIGINT
) RETURNS TABLE(
    plan_code VARCHAR,
    plan_name VARCHAR,
    monthly_price DECIMAL,
    commission_rate DECIMAL,
    referral_limit INT,
    property_limit INT,
    status subscriptions.subscription_status,
    expires_at TIMESTAMP WITH TIME ZONE
) AS $$
BEGIN
    RETURN QUERY
    SELECT
        sp.plan_code,
        sp.plan_name,
        sp.monthly_price,
        sp.commission_rate,
        sp.referral_limit_per_month,
        sp.property_publish_limit,
        us.status,
        us.expires_at
    FROM subscriptions.user_subscriptions us
    JOIN subscriptions.subscription_plans sp ON us.plan_id = sp.id
    WHERE us.user_id = p_user_id;
END;
$$ LANGUAGE plpgsql;

-- ============================================================================
-- COMMENTS FOR DOCUMENTATION
-- ============================================================================

COMMENT ON TABLE subscriptions.subscription_plans IS
'Catalog of available subscription plans (FREE, PRO, ELITE). Relatively static - plan changes should be versioned.';

COMMENT ON TABLE subscriptions.user_subscriptions IS
'Active subscription for each user (1:1 with users). FREE plan never expires (expires_at = NULL). Paid plans have monthly expiration.';

COMMENT ON TABLE subscriptions.payment_methods IS
'Tokenized payment methods for users. NEVER stores raw card numbers. Supports soft delete (deleted_at). Multiple methods per user allowed.';

COMMENT ON TABLE subscriptions.subscription_transactions IS
'IMMUTABLE audit log of all subscription transactions (upgrades, renewals, cancellations). Financial audit trail - NEVER delete.';

COMMENT ON COLUMN subscriptions.subscription_plans.commission_rate IS
'Commission rate as decimal (0.02=2%, 0.05=5%, 0.10=10%). Used for referral commission calculations.';

COMMENT ON COLUMN subscriptions.subscription_plans.referral_limit_per_month IS
'Maximum referrals per month. -1 or NULL = unlimited.';

COMMENT ON COLUMN subscriptions.user_subscriptions.expires_at IS
'Expiration timestamp. NULL for FREE plan (never expires). NOT NULL for paid plans (monthly renewal).';

COMMENT ON COLUMN subscriptions.user_subscriptions.auto_renew IS
'Auto-renew subscription on expiration? Requires payment_method_id to be set.';

COMMENT ON COLUMN subscriptions.payment_methods.gateway_token IS
'Tokenized payment method ID from gateway (e.g., Stripe payment method ID). NEVER store raw card numbers.';

COMMENT ON COLUMN subscriptions.payment_methods.deleted_at IS
'Soft delete timestamp. Payment methods are NEVER physically deleted for audit purposes.';

COMMENT ON FUNCTION subscriptions.auto_expire_subscriptions IS
'Auto-expires active subscriptions past their expiration date. Run daily: SELECT * FROM subscriptions.auto_expire_subscriptions();';

COMMENT ON FUNCTION subscriptions.get_user_plan IS
'Get current plan details for a user. Usage: SELECT * FROM subscriptions.get_user_plan(123);';

-- ============================================================================
-- ADD FOREIGN KEY TO PAYMENT_METHODS (after all tables are created)
-- ============================================================================
ALTER TABLE subscriptions.user_subscriptions
    ADD CONSTRAINT fk_user_subscription_payment_method
    FOREIGN KEY (payment_method_id)
    REFERENCES subscriptions.payment_methods(id)
    ON DELETE SET NULL;

COMMIT;

-- ============================================================================
-- ROLLBACK REFERENCE (for manual rollback only - NOT executed)
-- ============================================================================
-- DROP FUNCTION IF EXISTS subscriptions.get_user_plan(BIGINT);
-- DROP FUNCTION IF EXISTS subscriptions.auto_expire_subscriptions();
-- DROP FUNCTION IF EXISTS subscriptions.is_subscription_expired(BIGINT);
-- DROP FUNCTION IF EXISTS subscriptions.ensure_single_default_payment_method();
-- DROP FUNCTION IF EXISTS subscriptions.update_updated_at_column();
-- DROP TABLE IF EXISTS subscriptions.subscription_transactions CASCADE;
-- DROP TABLE IF EXISTS subscriptions.payment_methods CASCADE;
-- DROP TABLE IF EXISTS subscriptions.user_subscriptions CASCADE;
-- DROP TABLE IF EXISTS subscriptions.subscription_plans CASCADE;
