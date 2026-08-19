-- V92__backfill_all_users_to_free_plan.sql
-- Purpose: Asegurar que TODOS los usuarios activos estén en el plan FREE.
--
-- Reglas de negocio aplicadas:
--   - Plan: FREE (is_active = TRUE, el único operativo actualmente)
--   - Comisión del anfitrión (host): 15%  → definida en subscription_plans.host_commission_rate
--   - Comisión del referido (affiliate): dinámica por código, NO por plan
--       4% → sin propiedades publicadas
--       6% → con al menos 1 propiedad con status = 'PUBLISHED'
--     (lógica en UserSubscriptionAdapter.getAffiliateCommissionRate, no en DB)
--
-- Comportamiento:
--   - Usuarios SIN suscripción → INSERT con FREE plan
--   - Usuarios EN otro plan (PRO, ELITE) → UPDATE a FREE plan
--   - Usuarios ya en FREE → UPDATE idempotente (sin cambio efectivo)
--
-- Idempotente: seguro de re-ejecutar.
-- Depende de: V91 (FREE plan con host_commission_rate = 0.15 ya aplicado)

-- ─────────────────────────────────────────────────────────────────────────────
-- 1. UPSERT: INSERT para nuevos / UPDATE para todos los existentes
-- ─────────────────────────────────────────────────────────────────────────────
INSERT INTO subscriptions.user_subscriptions (
    user_id,
    plan_id,
    started_at,
    expires_at,
    status,
    auto_renew,
    payment_method_id,
    stripe_subscription_id,
    canceled_at,
    cancellation_reason,
    next_billing_date,
    last_renewal_at
)
SELECT
    u.id                                                                AS user_id,
    free_plan.id                                                        AS plan_id,
    COALESCE(u.created_at::TIMESTAMPTZ, NOW())                         AS started_at,
    NULL                                                                AS expires_at,            -- FREE no expira
    'ACTIVE'                                                            AS status,
    FALSE                                                               AS auto_renew,
    NULL                                                                AS payment_method_id,     -- FREE no requiere pago
    NULL                                                                AS stripe_subscription_id, -- FREE no tiene suscripción Stripe
    NULL                                                                AS canceled_at,
    NULL                                                                AS cancellation_reason,
    NULL                                                                AS next_billing_date,
    NULL                                                                AS last_renewal_at
FROM users.users u
CROSS JOIN (
    SELECT id FROM subscriptions.subscription_plans WHERE plan_code = 'FREE' LIMIT 1
) AS free_plan
ON CONFLICT (user_id) DO UPDATE
    SET plan_id               = EXCLUDED.plan_id,
        status                = 'ACTIVE',
        expires_at            = NULL,
        auto_renew            = FALSE,
        payment_method_id     = NULL,
        stripe_subscription_id = NULL,
        canceled_at           = NULL,
        cancellation_reason   = NULL,
        next_billing_date     = NULL,
        last_renewal_at       = NULL,
        updated_at            = NOW()
    -- Solo actualizar si realmente hay cambio (evita escrituras innecesarias)
    WHERE subscriptions.user_subscriptions.plan_id != EXCLUDED.plan_id
       OR subscriptions.user_subscriptions.status  != 'ACTIVE'
       OR subscriptions.user_subscriptions.auto_renew != FALSE;

-- ─────────────────────────────────────────────────────────────────────────────
-- 2. Sincronizar users.active_plan manualmente para los casos donde el trigger
--    puede no haberse disparado (ON CONFLICT UPDATE no dispara el trigger en
--    algunas versiones de PostgreSQL < 15 cuando no hay cambio en la fila).
-- ─────────────────────────────────────────────────────────────────────────────
UPDATE users.users
SET active_plan = 'FREE',
    updated_at  = NOW()
WHERE active_plan != 'FREE';

-- ─────────────────────────────────────────────────────────────────────────────
-- 3. Verificación post-migración
-- ─────────────────────────────────────────────────────────────────────────────
DO $$
DECLARE
    v_total_users       INTEGER;
    v_on_free_plan      INTEGER;
    v_without_sub       INTEGER;
    v_not_active_plan   INTEGER;
BEGIN
    SELECT COUNT(*) INTO v_total_users     FROM users.users;
    SELECT COUNT(*) INTO v_on_free_plan    FROM subscriptions.user_subscriptions us
                                            JOIN subscriptions.subscription_plans sp ON us.plan_id = sp.id
                                            WHERE sp.plan_code = 'FREE' AND us.status = 'ACTIVE';
    SELECT COUNT(*) INTO v_without_sub     FROM users.users u
                                            WHERE NOT EXISTS (
                                                SELECT 1 FROM subscriptions.user_subscriptions us WHERE us.user_id = u.id
                                            );
    SELECT COUNT(*) INTO v_not_active_plan FROM users.users WHERE active_plan != 'FREE';

    RAISE NOTICE '════════════════════════════════════════════';
    RAISE NOTICE 'V92 — Backfill FREE Plan — Resultado';
    RAISE NOTICE '  Total usuarios              : %', v_total_users;
    RAISE NOTICE '  En FREE plan (ACTIVE)       : %', v_on_free_plan;
    RAISE NOTICE '  Sin suscripción             : %', v_without_sub;
    RAISE NOTICE '  users.active_plan != FREE   : %', v_not_active_plan;

    IF v_without_sub > 0 OR v_not_active_plan > 0 THEN
        RAISE EXCEPTION 'V92 falló: quedan % usuarios sin suscripción y % con active_plan incorrecto',
            v_without_sub, v_not_active_plan;
    ELSE
        RAISE NOTICE '  ✓ Migración correcta. Comisión host: 15%% (definida en plan FREE).';
        RAISE NOTICE '  ✓ Comisión referido: dinámica (4%% base / 6%% con propiedad publicada) → manejada en código.';
    END IF;

    RAISE NOTICE '════════════════════════════════════════════';
END;
$$;
