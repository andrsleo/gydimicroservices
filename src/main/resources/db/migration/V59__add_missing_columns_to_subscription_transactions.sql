-- V59__add_missing_columns_to_subscription_transactions.sql
-- Description: Adds missing columns to subscription_transactions table
-- Author: GYDI Development Team
-- Date: 2025-12-16

-- Add stripe_payment_intent_id column for tracking Stripe Payment Intent IDs
ALTER TABLE subscriptions.subscription_transactions
ADD COLUMN stripe_payment_intent_id VARCHAR(255) UNIQUE;

-- Add stripe_charge_id column for tracking Stripe Charge IDs
ALTER TABLE subscriptions.subscription_transactions
ADD COLUMN stripe_charge_id VARCHAR(255);

-- Add processed_at column (synonym for completed_at, for compatibility)
ALTER TABLE subscriptions.subscription_transactions
ADD COLUMN processed_at TIMESTAMP WITH TIME ZONE;

-- Add updated_at column for tracking record updates
ALTER TABLE subscriptions.subscription_transactions
ADD COLUMN updated_at TIMESTAMP WITH TIME ZONE;

-- Add comments to document the columns
COMMENT ON COLUMN subscriptions.subscription_transactions.stripe_payment_intent_id IS 'Stripe Payment Intent ID';
COMMENT ON COLUMN subscriptions.subscription_transactions.stripe_charge_id IS 'Stripe Charge ID (after payment is confirmed)';
COMMENT ON COLUMN subscriptions.subscription_transactions.processed_at IS 'When the transaction was processed/completed';
COMMENT ON COLUMN subscriptions.subscription_transactions.updated_at IS 'Timestamp when this transaction was last updated';

-- Create indexes for faster lookups
CREATE INDEX idx_subscription_transactions_stripe_payment_intent_id
ON subscriptions.subscription_transactions(stripe_payment_intent_id)
WHERE stripe_payment_intent_id IS NOT NULL;

CREATE INDEX idx_subscription_transactions_stripe_charge_id
ON subscriptions.subscription_transactions(stripe_charge_id)
WHERE stripe_charge_id IS NOT NULL;
