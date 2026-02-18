-- Migration V83: Make to_plan_id nullable for CANCELLATION transactions
--
-- Purpose: Allow NULL values in to_plan_id for subscription cancellations
--          When canceling a subscription, there's no "destination plan"
--
-- Author: System
-- Date: 2026-02-16

-- Make to_plan_id nullable
ALTER TABLE subscriptions.subscription_transactions
    ALTER COLUMN to_plan_id DROP NOT NULL;

-- Add comment explaining when to_plan_id can be NULL
COMMENT ON COLUMN subscriptions.subscription_transactions.to_plan_id IS
    'Destination plan ID. NULL for CANCELLATION transactions (no destination plan). Required for UPGRADE and DOWNGRADE.';
