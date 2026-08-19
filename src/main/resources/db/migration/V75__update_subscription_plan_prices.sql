-- V73__update_subscription_plan_prices.sql
-- Purpose: Update subscription plan prices to new pricing structure
-- Author: GYDI Development Team
-- Date: 2026-02-03
-- Dependencies: V53__seed_subscription_plans.sql
--
-- PRICING CHANGES:
-- - FREE: $0 (unchanged)
-- - PRO: $29.99 → $19.00
-- - ELITE: $99.99 → $39.00

BEGIN;

-- ============================================================================
-- UPDATE SUBSCRIPTION PLAN PRICES
-- ============================================================================

-- Update PRO plan price from $29.99 to $19.00
UPDATE subscriptions.subscription_plans
SET monthly_price = 19.00,
    plan_description = 'Best for growing your business. Publish up to 50 properties and earn 5% commission on referrals.',
    updated_at = CURRENT_TIMESTAMP
WHERE plan_code = 'PRO';

-- Update ELITE plan price from $99.99 to $39.00
UPDATE subscriptions.subscription_plans
SET monthly_price = 39.00,
    plan_description = 'For power users. Unlimited properties and referrals with 10% commission rate.',
    updated_at = CURRENT_TIMESTAMP
WHERE plan_code = 'ELITE';

-- ============================================================================
-- VERIFICATION
-- ============================================================================

DO $$
DECLARE
    pro_price NUMERIC;
    elite_price NUMERIC;
BEGIN
    -- Verify PRO plan price
    SELECT monthly_price INTO pro_price
    FROM subscriptions.subscription_plans
    WHERE plan_code = 'PRO';

    IF pro_price = 19.00 THEN
        RAISE NOTICE 'PRO plan price updated successfully: $%.2f', pro_price;
    ELSE
        RAISE WARNING 'PRO plan price incorrect: $%.2f (expected $19.00)', pro_price;
    END IF;

    -- Verify ELITE plan price
    SELECT monthly_price INTO elite_price
    FROM subscriptions.subscription_plans
    WHERE plan_code = 'ELITE';

    IF elite_price = 39.00 THEN
        RAISE NOTICE 'ELITE plan price updated successfully: $%.2f', elite_price;
    ELSE
        RAISE WARNING 'ELITE plan price incorrect: $%.2f (expected $39.00)', elite_price;
    END IF;
END;
$$;

-- ============================================================================
-- DISPLAY UPDATED PLANS
-- ============================================================================

SELECT
    plan_code,
    plan_name,
    monthly_price,
    currency,
    commission_rate,
    referral_limit_per_month,
    property_publish_limit
FROM subscriptions.subscription_plans
WHERE is_active = true
ORDER BY display_order;

COMMIT;

-- ============================================================================
-- POST-MIGRATION NOTES
-- ============================================================================
--
-- IMPORTANT: After running this migration, you MUST update Stripe Price IDs
--
-- 1. Create new Price objects in Stripe:
--    - PRO: Create new price of $19.00/month
--    - ELITE: Create new price of $39.00/month
--
-- 2. Update stripe_price_id column in subscription_plans:
--    UPDATE subscriptions.subscription_plans
--    SET stripe_price_id = 'price_xxxxxxxxxxxxx'
--    WHERE plan_code = 'PRO';
--
--    UPDATE subscriptions.subscription_plans
--    SET stripe_price_id = 'price_yyyyyyyyyyyyy'
--    WHERE plan_code = 'ELITE';
--
-- 3. Archive old Stripe prices (do NOT delete them - keep for historical records)
--
-- 4. Update frontend constants in:
--    GydiFront/src/lib/constants/plans.ts
--
-- ============================================================================
