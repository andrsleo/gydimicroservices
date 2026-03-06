-- ============================================================
-- V95: Migrate stripe_customer_id (users.users) → stripe_connect_accounts
-- ============================================================
-- GYDI now uses a single Stripe identity per user stored in
-- commissions.stripe_connect_accounts:
--   stripe_account_id          acct_xxx  (receive payouts — affiliates)
--   stripe_platform_customer_id cus_xxx  (charge commissions — hosts)
-- ============================================================

-- 1. Add stripe_platform_customer_id to stripe_connect_accounts
ALTER TABLE commissions.stripe_connect_accounts
ADD COLUMN IF NOT EXISTS stripe_platform_customer_id VARCHAR(255) UNIQUE;

-- 2. Migrate data: users with cus_xxx that already have a Connect account
UPDATE commissions.stripe_connect_accounts sca
SET stripe_platform_customer_id = u.stripe_customer_id,
    updated_at = NOW()
FROM users.users u
WHERE sca.user_id = u.id
  AND u.stripe_customer_id IS NOT NULL
  AND sca.stripe_platform_customer_id IS NULL;

-- 3. Create stub records for users with cus_xxx but WITHOUT a Connect account
INSERT INTO commissions.stripe_connect_accounts (
    user_id, stripe_account_id, account_type, country,
    onboarding_completed, payouts_enabled, charges_enabled,
    details_submitted, verification_status,
    stripe_platform_customer_id, created_at, updated_at
)
SELECT
    u.id,
    'pending_' || u.id,
    'express',
    COALESCE(up.country, 'US'),
    false, false, false, false, 'unverified',
    u.stripe_customer_id,
    NOW(), NOW()
FROM users.users u
LEFT JOIN users.user_profile up ON up.user_id = u.id
WHERE u.stripe_customer_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM commissions.stripe_connect_accounts sca WHERE sca.user_id = u.id
  );

-- 4. Index for fast lookups by platform customer ID
CREATE INDEX IF NOT EXISTS idx_stripe_connect_platform_customer
    ON commissions.stripe_connect_accounts(stripe_platform_customer_id)
    WHERE stripe_platform_customer_id IS NOT NULL;

-- 5. Remove stripe_customer_id from users.users
ALTER TABLE users.users DROP COLUMN IF EXISTS stripe_customer_id;
