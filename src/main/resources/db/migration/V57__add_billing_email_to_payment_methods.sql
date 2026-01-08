-- ============================================
-- Migration: Add billing_email to payment_methods
-- Version: V57
-- Description: Adds billing_email column to payment_methods table
-- Author: System
-- Date: 2025-12-12
-- ============================================

-- Add billing_email column to payment_methods table
ALTER TABLE subscriptions.payment_methods
ADD COLUMN billing_email VARCHAR(255);

-- Add comment to document the column's purpose
COMMENT ON COLUMN subscriptions.payment_methods.billing_email IS
'Email address associated with this payment method';
