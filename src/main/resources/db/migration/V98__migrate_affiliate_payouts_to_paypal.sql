-- ============================================================
-- V98: Migrate affiliate payouts from Stripe Connect to PayPal
-- ============================================================
-- Business Context:
--   GYDI now uses PayPal Payouts API to pay affiliate commissions.
--   Stripe Connect (acct_xxx) is no longer used for affiliate payouts.
--   The stripe_connect_accounts table is REPURPOSED:
--     - Stripe Connect fields kept as-is (stripe_account_id, etc.) because
--       the table also stores stripe_platform_customer_id (cus_xxx) used to
--       charge host commissions — that flow is unchanged.
--     - New column paypal_email added: affiliates register their PayPal email
--       to receive commission payouts.
--
-- referral_commission table changes:
--   - Add paypal_payout_batch_id   VARCHAR(255)   — PayPal batch payout ID
--   - Add paypal_payout_item_id    VARCHAR(255)   — PayPal payout item ID
--   - Add payout_status            VARCHAR(50)    — PENDING/PROCESSING/COMPLETED/FAILED
--   - Add payout_processed_at      TIMESTAMPTZ    — When payout was processed
--   - Keep stripe_transfer_id and stripe_payout_id columns (nullable, historic data)
--
-- stripe_connect_accounts table changes:
--   - Add paypal_email  VARCHAR(255)   — Affiliate's PayPal email for payouts
-- ============================================================

BEGIN;

-- ============================================================
-- 1. ADD paypal_email TO commissions.stripe_connect_accounts
-- ============================================================

ALTER TABLE commissions.stripe_connect_accounts
    ADD COLUMN IF NOT EXISTS paypal_email VARCHAR(255);

COMMENT ON COLUMN commissions.stripe_connect_accounts.paypal_email IS
'PayPal email address used to receive affiliate commission payouts via PayPal Payouts API. Replaces Stripe Connect transfers for affiliate payouts.';

CREATE INDEX IF NOT EXISTS idx_stripe_connect_paypal_email
    ON commissions.stripe_connect_accounts(paypal_email)
    WHERE paypal_email IS NOT NULL;

-- ============================================================
-- 2. ADD PayPal PAYOUT FIELDS TO commissions.referral_commission
-- ============================================================

-- PayPal Batch Payout ID (PAYOUT-xxx returned by PayPal Payouts API)
ALTER TABLE commissions.referral_commission
    ADD COLUMN IF NOT EXISTS paypal_payout_batch_id VARCHAR(255);

-- PayPal Payout Item ID (individual item within a batch)
ALTER TABLE commissions.referral_commission
    ADD COLUMN IF NOT EXISTS paypal_payout_item_id VARCHAR(255);

-- Payout processing status (separate from commission lifecycle status)
ALTER TABLE commissions.referral_commission
    ADD COLUMN IF NOT EXISTS payout_status VARCHAR(50);

-- Timestamp when PayPal payout was processed
ALTER TABLE commissions.referral_commission
    ADD COLUMN IF NOT EXISTS payout_processed_at TIMESTAMP WITH TIME ZONE;

-- Check constraint for payout_status values
ALTER TABLE commissions.referral_commission
    ADD CONSTRAINT chk_referral_commission_payout_status
        CHECK (payout_status IS NULL OR payout_status IN ('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED'));

-- ============================================================
-- 3. ADD COMMENTS FOR DOCUMENTATION
-- ============================================================

COMMENT ON COLUMN commissions.referral_commission.paypal_payout_batch_id IS
'PayPal Batch Payout ID returned by POST /v1/payments/payouts. Format: PAYOUT-xxxxxxxxxxxxx. Set when payout is initiated.';

COMMENT ON COLUMN commissions.referral_commission.paypal_payout_item_id IS
'PayPal individual payout item ID within the batch. Used for status tracking and dispute resolution.';

COMMENT ON COLUMN commissions.referral_commission.payout_status IS
'PayPal payout processing status: PENDING (not yet sent), PROCESSING (sent to PayPal), COMPLETED (funds delivered), FAILED (PayPal rejected). Independent of commission lifecycle status.';

COMMENT ON COLUMN commissions.referral_commission.payout_processed_at IS
'Timestamp when the PayPal payout was initiated (not necessarily when funds arrived). Funds typically arrive within minutes for PayPal accounts.';

COMMENT ON COLUMN commissions.referral_commission.stripe_transfer_id IS
'DEPRECATED: Stripe Connect Transfer ID. No longer used for new commissions. Kept for historical data.';

COMMENT ON COLUMN commissions.referral_commission.stripe_payout_id IS
'DEPRECATED: Stripe Payout batch ID. No longer used for new commissions. Kept for historical data.';

-- ============================================================
-- 4. ADD INDEX FOR PAYOUT PROCESSING QUEUE
-- ============================================================

-- Find PAID commissions that need PayPal payout processing
CREATE INDEX IF NOT EXISTS idx_referral_commission_paypal_queue
    ON commissions.referral_commission(payout_status, status)
    WHERE status = 'PAID' AND (payout_status IS NULL OR payout_status = 'FAILED');

COMMIT;
