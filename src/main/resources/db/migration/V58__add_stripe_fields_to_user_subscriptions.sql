-- V58__add_stripe_fields_to_user_subscriptions.sql
-- Purpose: Add missing columns to user_subscriptions table that are present in UserSubscriptionEntity
-- These columns are needed for Stripe integration and subscription lifecycle management
-- Author: Antigravity
-- Date: 2025-12-16

BEGIN;

-- Add new columns
ALTER TABLE subscriptions.user_subscriptions
ADD COLUMN last_renewal_at TIMESTAMP WITH TIME ZONE,
ADD COLUMN next_billing_date TIMESTAMP WITH TIME ZONE,
ADD COLUMN stripe_subscription_id VARCHAR(255);

-- Add unique constraint for stripe_subscription_id
ALTER TABLE subscriptions.user_subscriptions
ADD CONSTRAINT uk_user_subscriptions_stripe_subscription_id UNIQUE (stripe_subscription_id);

COMMENT ON COLUMN subscriptions.user_subscriptions.last_renewal_at IS 'Timestamp of the last successful renewal payment';
COMMENT ON COLUMN subscriptions.user_subscriptions.next_billing_date IS 'Timestamp of the next scheduled billing date';
COMMENT ON COLUMN subscriptions.user_subscriptions.stripe_subscription_id IS 'Stripe subscription ID for recurring payments';

COMMIT;
