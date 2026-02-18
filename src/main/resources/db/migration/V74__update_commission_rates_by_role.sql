-- V74__update_commission_rates_by_role.sql
-- Purpose: Add role-specific commission rates (AFFILIATE vs HOST)
-- Author: GYDI Development Team
-- Date: 2026-02-04
-- Dependencies: V73__update_subscription_plan_prices.sql
--
-- COMMISSION STRUCTURE:
-- Users can be AFFILIATE, HOST, or BOTH
-- Commission rates differ based on role:
--
-- AFFILIATE (RECEIVES commission FROM platform):
--   FREE: Platform PAYS 2% of booking amount to affiliate
--   PRO: Platform PAYS 5% of booking amount to affiliate
--   ELITE: Platform PAYS 10% of booking amount to affiliate
--
-- HOST (PAYS commission TO platform):
--   FREE: Platform CHARGES 25% of booking amount from host
--   PRO: Platform CHARGES 20% of booking amount from host
--   ELITE: Platform CHARGES 15% of booking amount from host
--
-- EXAMPLE (PRO plan, $100 booking):
--   Host with PRO: Platform CHARGES $20 (20%) → Host receives $80
--   Affiliate with PRO: Platform PAYS $5 (5%) → Affiliate receives $5
--   Platform profit: $20 - $5 = $15

BEGIN;

-- ============================================================================
-- ADD NEW COLUMNS FOR ROLE-SPECIFIC COMMISSION RATES
-- ============================================================================

-- Add affiliate commission rate column
ALTER TABLE subscriptions.subscription_plans
ADD COLUMN IF NOT EXISTS affiliate_commission_rate NUMERIC(5, 4) DEFAULT 0.02;

-- Add host commission rate column
ALTER TABLE subscriptions.subscription_plans
ADD COLUMN IF NOT EXISTS host_commission_rate NUMERIC(5, 4) DEFAULT 0.25;

-- Add description for affiliate benefits
ALTER TABLE subscriptions.subscription_plans
ADD COLUMN IF NOT EXISTS affiliate_benefits TEXT;

-- Add description for host benefits
ALTER TABLE subscriptions.subscription_plans
ADD COLUMN IF NOT EXISTS host_benefits TEXT;

-- ============================================================================
-- UPDATE COMMISSION RATES BY PLAN
-- ============================================================================

-- FREE PLAN
UPDATE subscriptions.subscription_plans
SET
    commission_rate = 0.02,  -- Keep for backward compatibility (default to affiliate)
    affiliate_commission_rate = 0.02,  -- Platform PAYS 2% to affiliate per referred booking
    host_commission_rate = 0.25,       -- Platform CHARGES 25% from host per booking
    affiliate_benefits = '{"commission":"2%","referral_limit":"unlimited","access":"referral_links","subscription":"none"}',
    host_benefits = '{"commission":"25%","traffic":"additional","exclusivity":"none","payment":"per_booking"}',
    updated_at = CURRENT_TIMESTAMP
WHERE plan_code = 'FREE';

-- PRO PLAN
UPDATE subscriptions.subscription_plans
SET
    commission_rate = 0.05,  -- Keep for backward compatibility (default to affiliate)
    affiliate_commission_rate = 0.05,  -- Platform PAYS 5% to affiliate per referred booking
    host_commission_rate = 0.20,       -- Platform CHARGES 20% from host per booking
    affiliate_benefits = '{"commission":"5%","referral_limit":"unlimited","ideal_for":"constant_activity"}',
    host_benefits = '{"commission":"20%","traffic":"additional","exclusivity":"none","conditions":"better_per_booking"}',
    updated_at = CURRENT_TIMESTAMP
WHERE plan_code = 'PRO';

-- ELITE PLAN
UPDATE subscriptions.subscription_plans
SET
    commission_rate = 0.10,  -- Keep for backward compatibility (default to affiliate)
    affiliate_commission_rate = 0.10,  -- Platform PAYS 10% to affiliate per referred booking
    host_commission_rate = 0.15,       -- Platform CHARGES 15% from host per booking
    affiliate_benefits = '{"commission":"10%","referral_limit":"unlimited","ideal_for":"high_volume_partners"}',
    host_benefits = '{"commission":"15%","traffic":"additional","exclusivity":"none","priority_access":"new_features","profile":"highlighted"}',
    updated_at = CURRENT_TIMESTAMP
WHERE plan_code = 'ELITE';

-- ============================================================================
-- ADD COMMENTS FOR DOCUMENTATION
-- ============================================================================

COMMENT ON COLUMN subscriptions.subscription_plans.commission_rate IS
'Legacy commission rate field. For new implementations, use affiliate_commission_rate or host_commission_rate based on user role.';

COMMENT ON COLUMN subscriptions.subscription_plans.affiliate_commission_rate IS
'Commission rate for AFFILIATE role: percentage of booking amount that platform PAYS to affiliate for referrals.';

COMMENT ON COLUMN subscriptions.subscription_plans.host_commission_rate IS
'Platform fee rate for HOST role: percentage of booking amount that platform CHARGES to host (platform fee).';

COMMENT ON COLUMN subscriptions.subscription_plans.affiliate_benefits IS
'JSON object describing benefits for AFFILIATE role.';

COMMENT ON COLUMN subscriptions.subscription_plans.host_benefits IS
'JSON object describing benefits for HOST role.';

-- ============================================================================
-- VERIFICATION
-- ============================================================================

DO $$
DECLARE
    plan_record RECORD;
BEGIN
    FOR plan_record IN
        SELECT
            plan_code,
            plan_name,
            monthly_price,
            affiliate_commission_rate,
            host_commission_rate
        FROM subscriptions.subscription_plans
        WHERE is_active = true
        ORDER BY display_order
    LOOP
        RAISE NOTICE 'Plan: % ($%/month) - Affiliate: % | Host: %',
            plan_record.plan_code,
            plan_record.monthly_price,
            plan_record.affiliate_commission_rate * 100,
            plan_record.host_commission_rate * 100;
    END LOOP;
END;
$$;

-- ============================================================================
-- DISPLAY UPDATED PLANS WITH ROLE-SPECIFIC RATES
-- ============================================================================

SELECT
    plan_code,
    plan_name,
    monthly_price,
    affiliate_commission_rate AS affiliate_rate,
    host_commission_rate AS host_rate,
    CONCAT(affiliate_commission_rate * 100, '%') AS affiliate_display,
    CONCAT(host_commission_rate * 100, '%') AS host_display
FROM subscriptions.subscription_plans
WHERE is_active = true
ORDER BY display_order;

COMMIT;

-- ============================================================================
-- POST-MIGRATION NOTES
-- ============================================================================
--
-- ROLE-BASED COMMISSION CALCULATION:
--
-- 1. When calculating commission, check user's role:
--    - If user is AFFILIATE: use affiliate_commission_rate
--    - If user is HOST: use host_commission_rate
--    - If user is BOTH: calculate both commissions separately
--
-- 2. Example calculation (PRO plan, $100 booking):
--    - AFFILIATE receives: $100 × 5% = $5.00 (platform PAYS to affiliate)
--    - HOST pays: $100 × 20% = $20.00 (platform CHARGES from host)
--    - HOST receives: $100 - $20 = $80.00 (after platform fee)
--    - PLATFORM profit: $20.00 - $5.00 = $15.00
--
-- 3. Backend services that need updates:
--    - ReferralCommissionCalculator
--    - HostCommissionCalculator
--    - EarningsService
--
-- 4. Frontend updates:
--    - Plans display (show both commission rates)
--    - Earnings calculator
--    - Dashboard stats
--
-- ============================================================================
