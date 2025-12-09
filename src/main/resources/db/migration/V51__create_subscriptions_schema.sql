-- V51__create_subscriptions_schema.sql
-- Purpose: Create subscriptions schema and related ENUMs for subscription management
-- Author: Database Architect
-- Date: 2025-12-09
-- Dependencies: V1__init_schema.sql (users.users table)

BEGIN;

-- ============================================================================
-- SUBSCRIPTIONS SCHEMA
-- ============================================================================
-- Separating subscription/billing logic into its own schema follows DDD
-- bounded context principles and allows for independent scaling/security policies

CREATE SCHEMA IF NOT EXISTS subscriptions;

COMMENT ON SCHEMA subscriptions IS
    'Subscriptions domain - handles subscription plans, user subscriptions, billing, and payment methods';

-- ============================================================================
-- SUBSCRIPTION STATUS ENUM
-- ============================================================================
-- Lifecycle:
-- ACTIVE → user is subscribed and plan is valid (not expired)
-- EXPIRED → plan expired but not yet canceled (grace period, allows auto-renewal)
-- CANCELED → user explicitly canceled (no auto-renewal)
-- SUSPENDED → suspended due to payment failure or fraud

CREATE TYPE subscriptions.subscription_status AS ENUM (
    'ACTIVE',      -- Subscription is active and valid
    'EXPIRED',     -- Subscription expired (for paid plans), grace period
    'CANCELED',    -- User canceled subscription
    'SUSPENDED'    -- Suspended due to payment failure/fraud
);

COMMENT ON TYPE subscriptions.subscription_status IS
'Status of user subscription. ACTIVE=valid, EXPIRED=grace period, CANCELED=user canceled, SUSPENDED=payment failed';

-- ============================================================================
-- TRANSACTION TYPE ENUM
-- ============================================================================
-- Types of subscription transactions:
-- INITIAL_SUBSCRIPTION → user first subscribes to a plan (FREE plan at registration)
-- UPGRADE → user upgrades to higher-tier plan (FREE→PRO, PRO→ELITE, FREE→ELITE)
-- RENEWAL → monthly renewal of paid plan (PRO/ELITE)
-- DOWNGRADE → user downgrades to lower-tier plan (ELITE→PRO, ELITE→FREE, PRO→FREE)
-- CANCELLATION → user cancels paid plan (returns to FREE)
-- REACTIVATION → user reactivates previously canceled plan

CREATE TYPE subscriptions.transaction_type AS ENUM (
    'INITIAL_SUBSCRIPTION',  -- First subscription (usually FREE)
    'UPGRADE',               -- Upgrade to higher plan
    'RENEWAL',               -- Monthly renewal
    'DOWNGRADE',             -- Downgrade to lower plan
    'CANCELLATION',          -- Cancel paid plan
    'REACTIVATION'           -- Reactivate canceled plan
);

COMMENT ON TYPE subscriptions.transaction_type IS
'Type of subscription transaction. Tracks all plan changes and billing events.';

-- ============================================================================
-- TRANSACTION STATUS ENUM
-- ============================================================================
-- Status of payment transaction:
-- PENDING → transaction created, awaiting payment processing
-- PROCESSING → payment gateway is processing
-- COMPLETED → payment successful, subscription updated
-- FAILED → payment failed
-- REFUNDED → transaction was refunded
-- CANCELED → transaction canceled before processing

CREATE TYPE subscriptions.transaction_status AS ENUM (
    'PENDING',      -- Transaction created, awaiting processing
    'PROCESSING',   -- Payment being processed
    'COMPLETED',    -- Transaction completed successfully
    'FAILED',       -- Transaction failed
    'REFUNDED',     -- Transaction refunded
    'CANCELED'      -- Transaction canceled
);

COMMENT ON TYPE subscriptions.transaction_status IS
'Status of subscription transaction. PENDING→PROCESSING→COMPLETED or FAILED.';

-- ============================================================================
-- PAYMENT METHOD TYPE ENUM
-- ============================================================================
-- Supported payment method types

CREATE TYPE subscriptions.payment_method_type AS ENUM (
    'CREDIT_CARD',   -- Credit card (Visa, MC, Amex, etc.)
    'DEBIT_CARD',    -- Debit card
    'PAYPAL',        -- PayPal account
    'STRIPE',        -- Stripe payment method
    'OTHER'          -- Other payment methods
);

COMMENT ON TYPE subscriptions.payment_method_type IS
'Type of payment method. Used for billing user subscriptions.';

-- ============================================================================
-- GRANT PERMISSIONS (optional - adjust as needed)
-- ============================================================================
-- Grant usage to application user
-- GRANT USAGE ON SCHEMA subscriptions TO gydi_app_user;
-- GRANT SELECT, INSERT, UPDATE ON ALL TABLES IN SCHEMA subscriptions TO gydi_app_user;
-- GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA subscriptions TO gydi_app_user;

COMMIT;

-- ============================================================================
-- ROLLBACK REFERENCE (for manual rollback only - NOT executed)
-- ============================================================================
-- DROP TYPE IF EXISTS subscriptions.payment_method_type CASCADE;
-- DROP TYPE IF EXISTS subscriptions.transaction_status CASCADE;
-- DROP TYPE IF EXISTS subscriptions.transaction_type CASCADE;
-- DROP TYPE IF EXISTS subscriptions.subscription_status CASCADE;
-- DROP SCHEMA IF EXISTS subscriptions CASCADE;
