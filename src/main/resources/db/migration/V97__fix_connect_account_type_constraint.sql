-- ============================================================
-- V97: Fix account_type check constraint for JPA enum values
-- ============================================================
-- JPA @Enumerated(EnumType.STRING) stores enum names in UPPERCASE
-- (STANDARD, EXPRESS, CUSTOM), but V81 constraint requires lowercase.
-- This migration drops the old constraint and adds one that accepts
-- the uppercase values that JPA actually persists.
-- ============================================================

-- Step 1: Drop old constraint FIRST so the UPDATE below is not blocked
ALTER TABLE commissions.stripe_connect_accounts
    DROP CONSTRAINT IF EXISTS chk_stripe_connect_account_type;

-- Step 2: Migrate existing rows to uppercase (now safe, no constraint)
UPDATE commissions.stripe_connect_accounts
SET account_type = UPPER(account_type)
WHERE account_type IN ('standard', 'express', 'custom');

-- Step 3: Add new constraint accepting uppercase values (matches JPA EnumType.STRING)
ALTER TABLE commissions.stripe_connect_accounts
    ADD CONSTRAINT chk_stripe_connect_account_type
        CHECK (account_type IN ('STANDARD', 'EXPRESS', 'CUSTOM'));
