-- V53__seed_subscription_plans.sql
-- Purpose: Insert initial subscription plan data (FREE, PRO, ELITE)
-- Author: Database Architect
-- Date: 2025-12-09
-- Dependencies: V52__create_subscription_plans_and_tables.sql

BEGIN;

-- ============================================================================
-- SEED SUBSCRIPTION PLANS
-- ============================================================================
-- Insert the 3 subscription plans: FREE, PRO, ELITE
-- These match the SubscriptionPlan enum in Java code

INSERT INTO subscriptions.subscription_plans (
    plan_code,
    plan_name,
    plan_description,
    monthly_price,
    currency,
    commission_rate,
    referral_limit_per_month,
    property_publish_limit,
    is_active,
    is_featured,
    display_order
) VALUES
    -- FREE PLAN (default for all new users)
    (
        'FREE',
        'Free Plan',
        'Perfect for getting started. Publish up to 10 properties and earn 2% commission on referrals.',
        0.00,
        'USD',
        0.02,  -- 2% commission
        50,    -- 50 referrals per month
        10,    -- 10 properties
        TRUE,
        FALSE,
        1
    ),
    -- PRO PLAN (mid-tier)
    (
        'PRO',
        'Pro Plan',
        'Best for growing your business. Publish up to 50 properties and earn 5% commission on referrals.',
        29.99,
        'USD',
        0.05,  -- 5% commission
        200,   -- 200 referrals per month
        50,    -- 50 properties
        TRUE,
        TRUE,  -- Featured plan
        2
    ),
    -- ELITE PLAN (premium)
    (
        'ELITE',
        'Elite Plan',
        'For power users. Unlimited properties and referrals with 10% commission rate.',
        99.99,
        'USD',
        0.10,  -- 10% commission
        2147483647,  -- Unlimited (INT MAX)
        2147483647,  -- Unlimited (INT MAX)
        TRUE,
        FALSE,
        3
    )
ON CONFLICT (plan_code) DO NOTHING;

-- ============================================================================
-- VERIFICATION QUERY
-- ============================================================================
-- Verify plans were inserted correctly
DO $$
DECLARE
    plan_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO plan_count
    FROM subscriptions.subscription_plans
    WHERE plan_code IN ('FREE', 'PRO', 'ELITE');

    IF plan_count = 3 THEN
        RAISE NOTICE 'Successfully inserted 3 subscription plans (FREE, PRO, ELITE)';
    ELSE
        RAISE WARNING 'Expected 3 plans, found %', plan_count;
    END IF;
END;
$$;

-- ============================================================================
-- CREATE DEFAULT FREE SUBSCRIPTIONS FOR EXISTING USERS
-- ============================================================================
-- All existing users should have a FREE subscription by default
-- This ensures every user has exactly one subscription

INSERT INTO subscriptions.user_subscriptions (
    user_id,
    plan_id,
    started_at,
    expires_at,
    status,
    auto_renew
)
SELECT
    u.id AS user_id,
    (SELECT id FROM subscriptions.subscription_plans WHERE plan_code = 'FREE') AS plan_id,
    u.created_at AS started_at,
    NULL AS expires_at,  -- FREE plan never expires
    'ACTIVE' AS status,
    FALSE AS auto_renew
FROM users.users u
WHERE NOT EXISTS (
    -- Only insert if user doesn't already have a subscription
    SELECT 1
    FROM subscriptions.user_subscriptions us
    WHERE us.user_id = u.id
)
ON CONFLICT (user_id) DO NOTHING;

-- ============================================================================
-- VERIFICATION QUERY
-- ============================================================================
-- Verify all users have subscriptions
DO $$
DECLARE
    user_count INTEGER;
    subscription_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO user_count FROM users.users;
    SELECT COUNT(*) INTO subscription_count FROM subscriptions.user_subscriptions;

    IF user_count = subscription_count THEN
        RAISE NOTICE 'All % users have subscriptions', user_count;
    ELSE
        RAISE WARNING 'User count (%) != Subscription count (%)', user_count, subscription_count;
    END IF;
END;
$$;

-- ============================================================================
-- SYNC users.users.active_plan WITH subscriptions SCHEMA
-- ============================================================================
-- Update the existing active_plan column in users.users to match new subscriptions
-- This maintains backward compatibility with existing code

UPDATE users.users u
SET active_plan = sp.plan_code
FROM subscriptions.user_subscriptions us
JOIN subscriptions.subscription_plans sp ON us.plan_id = sp.id
WHERE u.id = us.user_id
  AND us.status = 'ACTIVE'
  AND u.active_plan != sp.plan_code;

-- ============================================================================
-- CREATE TRIGGER TO KEEP users.active_plan IN SYNC
-- ============================================================================
-- When user_subscriptions changes, automatically update users.active_plan
-- This ensures backward compatibility with existing code that reads active_plan

CREATE OR REPLACE FUNCTION subscriptions.sync_user_active_plan()
RETURNS TRIGGER AS $$
DECLARE
    v_plan_code VARCHAR(20);
BEGIN
    -- Get plan code for new subscription
    SELECT plan_code INTO v_plan_code
    FROM subscriptions.subscription_plans
    WHERE id = NEW.plan_id;

    -- Update users.active_plan only if subscription is ACTIVE
    IF NEW.status = 'ACTIVE' THEN
        UPDATE users.users
        SET active_plan = v_plan_code
        WHERE id = NEW.user_id;
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER sync_user_active_plan_trigger
AFTER INSERT OR UPDATE ON subscriptions.user_subscriptions
FOR EACH ROW EXECUTE FUNCTION subscriptions.sync_user_active_plan();

-- ============================================================================
-- COMMENTS
-- ============================================================================

COMMENT ON FUNCTION subscriptions.sync_user_active_plan IS
'Automatically syncs users.active_plan column when user_subscriptions changes. Maintains backward compatibility.';

COMMIT;

-- ============================================================================
-- POST-MIGRATION VERIFICATION QUERIES
-- ============================================================================
-- Run these queries after migration to verify success:

-- 1. Check all plans exist
-- SELECT * FROM subscriptions.subscription_plans ORDER BY display_order;

-- 2. Check all users have subscriptions
-- SELECT COUNT(*) AS users_with_subscriptions
-- FROM subscriptions.user_subscriptions;

-- 3. Check plan distribution
-- SELECT sp.plan_code, COUNT(*) AS user_count
-- FROM subscriptions.user_subscriptions us
-- JOIN subscriptions.subscription_plans sp ON us.plan_id = sp.id
-- GROUP BY sp.plan_code
-- ORDER BY sp.display_order;

-- 4. Verify users.active_plan is in sync
-- SELECT u.id, u.email, u.active_plan AS old_plan, sp.plan_code AS new_plan
-- FROM users.users u
-- JOIN subscriptions.user_subscriptions us ON u.id = us.user_id
-- JOIN subscriptions.subscription_plans sp ON us.plan_id = sp.id
-- WHERE u.active_plan != sp.plan_code;  -- Should return 0 rows
