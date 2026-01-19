-- V54__add_stripe_invoice_id_to_transactions.sql
-- Description: Adds stripe_invoice_id column to subscription_transactions table
-- Author: GYDI Development Team
-- Date: 2025-01-10

-- Add stripe_invoice_id column for tracking Stripe invoice IDs
ALTER TABLE subscriptions.subscription_transactions
ADD COLUMN stripe_invoice_id VARCHAR(255);

-- Add comment to document the column
COMMENT ON COLUMN subscriptions.subscription_transactions.stripe_invoice_id IS 'Stripe Invoice ID for subscription renewals';

-- Create index for faster lookups by invoice ID
CREATE INDEX idx_subscription_transactions_stripe_invoice_id
ON subscriptions.subscription_transactions(stripe_invoice_id)
WHERE stripe_invoice_id IS NOT NULL;
