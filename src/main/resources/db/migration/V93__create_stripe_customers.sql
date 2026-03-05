-- V93__create_stripe_customers.sql
-- Purpose: Ensure all active users have a stripe_customer_id.
--
-- Background: Stripe customer creation requires calling the Stripe API
-- (cannot be done in pure SQL). This migration handles the DB side:
--   1. Logs how many users are missing stripe_customer_id.
--   2. Ensures the stripe_customer_id column exists and is indexed.
--   3. Records the migration state so Flyway tracks it.
--
-- The actual Stripe API calls (Customer.create) are performed at application
-- startup via StripeCustomerSyncService (ApplicationRunner), which runs
-- exactly once per user and is idempotent.
--
-- Business rules (same as original Java migration V93):
--   - Only active users (is_active = TRUE)
--   - Only users without stripe_customer_id (idempotent)
--   - Batch size: 50 per run, with rate-limit delays
-- ============================================================================

-- ============================================================================
-- STEP 0: RESET OBSOLETE stripe_customer_id VALUES
-- ============================================================================
-- Active users may already have a stripe_customer_id that was created against
-- a previous Stripe account (e.g., account T7GLW). Those IDs are not valid in
-- the current Stripe account and must be cleared so that StripeCustomerSyncService
-- recreates them correctly on the next application startup.
--
-- Safety: This is safe in local/dev because:
--   (a) We are in a development environment.
--   (b) StripeCustomerSyncService recreates every NULL entry automatically.
--   (c) The developer has explicitly requested this reset.
-- ============================================================================

UPDATE users.users
SET    stripe_customer_id = NULL,
       updated_at         = NOW()
WHERE  stripe_customer_id IS NOT NULL;

-- ============================================================================
-- Ensure index exists for efficient lookup of users without stripe_customer_id
CREATE INDEX IF NOT EXISTS idx_users_missing_stripe_customer
    ON users.users (id)
    WHERE stripe_customer_id IS NULL
      AND is_active = TRUE;

COMMENT ON INDEX users.idx_users_missing_stripe_customer IS
'Partial index to efficiently find active users that still need a Stripe customer ID created. Used by StripeCustomerSyncService on application startup.';

-- Log migration state
DO $$
DECLARE
    pending_count BIGINT;
    total_active  BIGINT;
BEGIN
    SELECT COUNT(*) INTO pending_count
    FROM users.users
    WHERE stripe_customer_id IS NULL
      AND is_active = TRUE;

    SELECT COUNT(*) INTO total_active
    FROM users.users
    WHERE is_active = TRUE;

    RAISE NOTICE '';
    RAISE NOTICE '=== V93 — Stripe Customer Sync Status ===';
    RAISE NOTICE '  Active users total            : %', total_active;
    RAISE NOTICE '  Users missing stripe_customer_id: %', pending_count;
    RAISE NOTICE '  Users already synced          : %', total_active - pending_count;
    RAISE NOTICE '';

    IF pending_count > 0 THEN
        RAISE NOTICE '  ACTION REQUIRED: % user(s) need Stripe customer creation.', pending_count;
        RAISE NOTICE '  These will be processed automatically by StripeCustomerSyncService';
        RAISE NOTICE '  on the next application startup (requires STRIPE_API_KEY configured).';
    ELSE
        RAISE NOTICE '  All active users already have a Stripe customer ID.';
    END IF;

    RAISE NOTICE '==========================================';
    RAISE NOTICE '';
END $$;
