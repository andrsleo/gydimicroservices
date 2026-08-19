-- ============================================
-- Migration: Add Stripe Price ID to Subscription Plans
-- Version: V56
-- Description: Adds stripe_price_id column to subscription_plans table for Stripe integration
-- Author: System
-- Date: 2025-12-11
-- ============================================

-- Add stripe_price_id column to subscription_plans table
-- This column stores the Stripe Price ID (price_xxxxx) for each plan
-- It enables linking plans in our database with their Stripe Price records
ALTER TABLE subscriptions.subscription_plans
ADD COLUMN stripe_price_id VARCHAR(255);

-- Add unique constraint to prevent duplicate Stripe prices
-- This ensures one plan can only be associated with one Stripe price
ALTER TABLE subscriptions.subscription_plans
ADD CONSTRAINT uk_subscription_plans_stripe_price_id
UNIQUE (stripe_price_id);

-- Create index for efficient lookups by Stripe Price ID
-- This is useful for webhook processing where Stripe sends price IDs
CREATE INDEX idx_subscription_plans_stripe_price_id
ON subscriptions.subscription_plans(stripe_price_id)
WHERE stripe_price_id IS NOT NULL;

-- Add comment to document the column's purpose
COMMENT ON COLUMN subscriptions.subscription_plans.stripe_price_id IS
'Stripe Price ID (price_xxxxx) - Links this plan to its Stripe Price record. Created when plan is synced with Stripe. NULL for FREE plan or plans not yet synced.';
