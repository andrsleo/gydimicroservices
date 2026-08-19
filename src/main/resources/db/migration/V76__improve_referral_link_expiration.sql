-- =========================================================
-- Migration V70: Improve Referral Link Expiration System
-- =========================================================
-- Purpose: Update referral link expiration to be based on user subscription plan
-- Context: PHASE 1 + 2 implementation
--   - PHASE 1: Expiration based on subscription plan (FREE: 30d, PRO: 90d, ELITE: 365d)
--   - PHASE 2: Manual renewal support (renew existing links with new expiration)
--
-- Author: GYDI Development Team
-- Date: 2026-02-05
-- Related Use Cases:
--   - GenerateReferralLinkUseCase (calculates expiration from user's plan)
--   - RenewReferralLinkUseCase (renews existing links)
--   - ReferralLinkExpirationScheduler (cron job to mark expired links)
-- =========================================================

-- Step 1: Update existing referral links to have expiration based on user's current plan
-- Uses proper JOINs: users → user_subscriptions → plans
-- FREE plan: 30 days, PRO plan: 90 days, ELITE plan: 365 days

UPDATE referrals.referral_links rl
SET expires_at = CASE
    WHEN sp.plan_code = 'FREE' THEN rl.created_at + INTERVAL '30 days'
    WHEN sp.plan_code = 'PRO' THEN rl.created_at + INTERVAL '90 days'
    WHEN sp.plan_code = 'ELITE' THEN rl.created_at + INTERVAL '365 days'
    ELSE rl.created_at + INTERVAL '30 days' -- Default to FREE if plan is NULL
END
FROM subscriptions.user_subscriptions us
INNER JOIN subscriptions.subscription_plans sp ON us.plan_id = sp.id
WHERE rl.affiliate_id = us.user_id
  AND rl.status = 'ACTIVE'
  AND rl.deleted_at IS NULL
  AND us.status = 'ACTIVE'; -- Solo considerar suscripciones activas

-- Step 2: Create index to optimize scheduler query (findActiveExpired)
-- The scheduler runs every hour to find: status='ACTIVE' AND expires_at < NOW()
-- This index speeds up that query significantly
CREATE INDEX IF NOT EXISTS idx_referral_links_active_expiration
    ON referrals.referral_links(status, expires_at)
    WHERE status = 'ACTIVE' AND deleted_at IS NULL;

-- Step 3: Add comment explaining the new expiration logic
COMMENT ON COLUMN referrals.referral_links.expires_at IS
'Expiration date based on user subscription plan at creation time:
FREE: 30 days, PRO: 90 days, ELITE: 365 days.
Can be renewed manually via RenewReferralLinkUseCase which updates this date
and generates new encrypted_token with updated expiration.';

-- Step 4: Add index to optimize findActiveLink query (used frequently)
-- This query is used when generating new links and resolving tokens
CREATE INDEX IF NOT EXISTS idx_referral_links_active_lookup
    ON referrals.referral_links(affiliate_id, property_id, status)
    WHERE status = 'ACTIVE' AND deleted_at IS NULL;

-- =========================================================
-- Scheduler Information:
-- =========================================================
-- ReferralLinkExpirationScheduler runs every hour (cron: "0 0 * * * *")
-- Queries: SELECT * FROM referrals.referral_links
--          WHERE status = 'ACTIVE' AND deleted_at IS NULL AND expires_at < NOW()
-- Action: Updates status to 'EXPIRED' for matched records
-- =========================================================

-- =========================================================
-- Rollback Plan:
-- =========================================================
-- DROP INDEX IF EXISTS referrals.idx_referral_links_active_expiration;
-- DROP INDEX IF EXISTS referrals.idx_referral_links_active_lookup;
-- -- Optionally revert expires_at to original 90 days:
-- UPDATE referrals.referral_links
-- SET expires_at = created_at + INTERVAL '90 days'
-- WHERE status = 'ACTIVE' AND deleted_at IS NULL;
-- =========================================================
