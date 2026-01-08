-- ============================================
-- Migration: Add Stripe Customer ID to Users
-- Version: V55
-- Description: Adds stripe_customer_id column to users table for Stripe integration
-- Author: System
-- Date: 2025-12-11
-- ============================================

-- Add stripe_customer_id column to users table
-- This column stores the Stripe Customer ID (cus_xxxxx) for each user
-- It enables linking users in our database with their Stripe Customer records
ALTER TABLE users.users
ADD COLUMN stripe_customer_id VARCHAR(255);

-- Add unique constraint to prevent duplicate Stripe customers
-- This ensures one user can only be associated with one Stripe customer
ALTER TABLE users.users
ADD CONSTRAINT uk_users_stripe_customer_id
UNIQUE (stripe_customer_id);

-- Create index for efficient lookups by Stripe Customer ID
-- This is critical for webhook processing where Stripe sends customer IDs
CREATE INDEX idx_users_stripe_customer_id
ON users.users(stripe_customer_id)
WHERE stripe_customer_id IS NOT NULL;

-- Add comment to document the column's purpose
COMMENT ON COLUMN users.users.stripe_customer_id IS
'Stripe Customer ID (cus_xxxxx) - Links this user to their Stripe Customer record. Created when user first interacts with payment features. NULL for users who have never used paid features.';
