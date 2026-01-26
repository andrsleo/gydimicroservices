-- =========================================================
-- Migration V72: Convert subscriptions.subscription_status from ENUM to VARCHAR
-- =========================================================
-- Purpose: Fix PostgreSQL ENUM type mismatch error with Hibernate
-- Context: subscriptions.subscription_status ENUM causes casting errors with @Enumerated(EnumType.STRING)
-- Solution: Convert to VARCHAR(50) with CHECK constraint for validation
--
-- Author: GYDI Development Team
-- Date: 2026-01-26
-- Issue: PostgreSQL ENUM compatibility with Hibernate JPA
-- ENUM values: 'ACTIVE', 'EXPIRED', 'CANCELED', 'SUSPENDED'
-- =========================================================

-- Step 1: Drop partial indexes that have WHERE clauses referencing status ENUM
-- This index was created in V52
DROP INDEX IF EXISTS subscriptions.idx_user_subscriptions_expiring;

-- Step 2: Drop functions that reference subscription_status ENUM type
-- These functions were created in V52
DROP FUNCTION IF EXISTS subscriptions.auto_expire_subscriptions() CASCADE;
DROP FUNCTION IF EXISTS subscriptions.get_user_plan(BIGINT) CASCADE;

-- Step 3: Remove default value (eliminates ENUM dependency)
-- The column has DEFAULT 'ACTIVE' which depends on the ENUM type
ALTER TABLE subscriptions.user_subscriptions
    ALTER COLUMN status DROP DEFAULT;

-- Step 3.5: Drop CHECK constraint that references the ENUM type
-- This constraint 'chk_canceled_status' compares status with 'CANCELED' (enum value)
-- It must be dropped before converting the column to VARCHAR
ALTER TABLE subscriptions.user_subscriptions
    DROP CONSTRAINT IF EXISTS chk_canceled_status;

-- Step 4: Convert column from ENUM to VARCHAR
ALTER TABLE subscriptions.user_subscriptions
    ALTER COLUMN status TYPE VARCHAR(50) USING status::TEXT;

-- Step 5: Drop the implicit CAST created in V61 (must drop before dropping ENUM type)
DROP CAST IF EXISTS (varchar AS subscriptions.subscription_status);

-- Step 6: Drop the ENUM type (now safe)
DROP TYPE IF EXISTS subscriptions.subscription_status;

-- Step 7: Add CHECK constraints for validation
ALTER TABLE subscriptions.user_subscriptions
    ADD CONSTRAINT chk_user_subscriptions_status
    CHECK (status IN ('ACTIVE', 'EXPIRED', 'CANCELED', 'SUSPENDED'));

-- Recreate the chk_canceled_status constraint (now working with VARCHAR)
ALTER TABLE subscriptions.user_subscriptions
    ADD CONSTRAINT chk_canceled_status
    CHECK (
        (status = 'CANCELED' AND canceled_at IS NOT NULL) OR
        (status != 'CANCELED' AND canceled_at IS NULL)
    );

-- Step 8: Re-add default value (as VARCHAR literal)
-- Restore the DEFAULT 'ACTIVE' that existed before
ALTER TABLE subscriptions.user_subscriptions
    ALTER COLUMN status SET DEFAULT 'ACTIVE';

-- Step 9: Add index for query performance (if column is frequently queried)
CREATE INDEX IF NOT EXISTS idx_user_subscriptions_status
    ON subscriptions.user_subscriptions(status)
    WHERE status IS NOT NULL;

-- Step 10: Recreate partial index that was dropped in Step 1
-- Now it works with VARCHAR instead of ENUM
CREATE INDEX idx_user_subscriptions_expiring
ON subscriptions.user_subscriptions(expires_at, status, auto_renew)
WHERE status = 'ACTIVE' AND expires_at IS NOT NULL;

-- Step 11: Recreate functions that were dropped in Step 2
-- Now they work with VARCHAR instead of ENUM
CREATE OR REPLACE FUNCTION subscriptions.auto_expire_subscriptions()
RETURNS TABLE(expired_count INTEGER) AS $$
DECLARE
    updated_rows INTEGER;
BEGIN
    WITH updated AS (
        UPDATE subscriptions.user_subscriptions
        SET status = 'EXPIRED',
            updated_at = CURRENT_TIMESTAMP
        WHERE status = 'ACTIVE'
          AND expires_at IS NOT NULL
          AND expires_at < CURRENT_TIMESTAMP
        RETURNING id
    )
    SELECT COUNT(*)::INTEGER INTO updated_rows FROM updated;

    RAISE NOTICE 'Auto-expired % subscriptions', updated_rows;
    RETURN QUERY SELECT updated_rows;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION subscriptions.get_user_plan(
    p_user_id BIGINT
) RETURNS TABLE(
    plan_code VARCHAR,
    plan_name VARCHAR,
    monthly_price DECIMAL,
    commission_rate DECIMAL,
    referral_limit INT,
    property_limit INT,
    status VARCHAR,
    expires_at TIMESTAMP WITH TIME ZONE
) AS $$
BEGIN
    RETURN QUERY
    SELECT
        sp.plan_code,
        sp.plan_name,
        sp.monthly_price,
        sp.commission_rate,
        sp.referral_limit_per_month,
        sp.property_publish_limit,
        us.status,
        us.expires_at
    FROM subscriptions.user_subscriptions us
    JOIN subscriptions.subscription_plans sp ON us.plan_id = sp.id
    WHERE us.user_id = p_user_id;
END;
$$ LANGUAGE plpgsql;

-- =========================================================
-- Rollback Plan:
-- =========================================================
-- DROP INDEX IF EXISTS idx_user_subscriptions_status;
-- ALTER TABLE subscriptions.user_subscriptions DROP CONSTRAINT IF EXISTS chk_user_subscriptions_status;
-- CREATE TYPE subscriptions.subscription_status AS ENUM ('ACTIVE', 'EXPIRED', 'CANCELED', 'SUSPENDED');
-- ALTER TABLE subscriptions.user_subscriptions
--     ALTER COLUMN status TYPE subscriptions.subscription_status USING status::subscriptions.subscription_status;
-- =========================================================

COMMENT ON COLUMN subscriptions.user_subscriptions.status IS 'Subscription status: ACTIVE (valid), EXPIRED (grace period), CANCELED (user canceled), SUSPENDED (payment failed) - converted from ENUM to VARCHAR for Hibernate compatibility';
