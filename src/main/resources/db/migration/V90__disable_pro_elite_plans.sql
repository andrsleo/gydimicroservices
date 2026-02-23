-- V90__disable_pro_elite_plans.sql
-- Purpose: Disable PRO and ELITE subscription plans. Only FREE remains active.
-- Date: 2026-02-23
--
-- REASON: Temporarily hiding paid plans while capturing initial user base.
-- The data is preserved — to RE-ENABLE, create a V92 migration with:
--   UPDATE subscriptions.subscription_plans
--   SET is_active = true, updated_at = CURRENT_TIMESTAMP
--   WHERE plan_code IN ('PRO', 'ELITE');
--
-- PRO_ELITE_DISABLED: Tag to search when reactivating paid plans.

UPDATE subscriptions.subscription_plans
SET is_active = false,
    updated_at = CURRENT_TIMESTAMP
WHERE plan_code IN ('PRO', 'ELITE');
