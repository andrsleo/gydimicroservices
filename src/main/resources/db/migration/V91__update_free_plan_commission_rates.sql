-- V91__update_free_plan_commission_rates.sql
-- Purpose: Update FREE plan commission rates.
--   AFFILIATE: 2% -> 6%  (platform pays more to affiliates)
--   HOST:      25% -> 15% (platform charges less to hosts)
-- Date: 2026-02-23
-- Depends on: V74__update_commission_rates_by_role.sql

UPDATE subscriptions.subscription_plans
SET
    commission_rate            = 0.06,
    affiliate_commission_rate  = 0.06,
    host_commission_rate       = 0.15,
    updated_at                 = CURRENT_TIMESTAMP
WHERE plan_code = 'FREE';
