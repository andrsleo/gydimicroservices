-- ============================================================
-- V96: Add missing columns to stripe_connect_accounts
-- ============================================================
-- V81 defined the table but missed two columns that the domain
-- model uses for affiliate onboarding status tracking:
--   details_submitted   → Stripe's account.details_submitted field
--   verification_status → derived status (unverified, pending, verified, disabled)
-- ============================================================

ALTER TABLE commissions.stripe_connect_accounts
    ADD COLUMN IF NOT EXISTS details_submitted BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS verification_status VARCHAR(50) NOT NULL DEFAULT 'unverified';

-- Backfill: if onboarding_completed is already true, details_submitted should also be true
UPDATE commissions.stripe_connect_accounts
SET details_submitted = TRUE
WHERE onboarding_completed = TRUE
  AND details_submitted = FALSE;

-- Backfill: if payouts_enabled is true, mark as verified
UPDATE commissions.stripe_connect_accounts
SET verification_status = 'verified'
WHERE payouts_enabled = TRUE
  AND verification_status = 'unverified';

-- Backfill: if details submitted but not yet payouts enabled, mark as pending
UPDATE commissions.stripe_connect_accounts
SET verification_status = 'pending'
WHERE details_submitted = TRUE
  AND payouts_enabled = FALSE
  AND verification_status = 'unverified';
