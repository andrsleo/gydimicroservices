-- =========================================================
-- Migration V67: Convert referrals.referral_link_status from ENUM to VARCHAR
-- =========================================================
-- Purpose: Fix PostgreSQL ENUM type mismatch error with Hibernate
-- Context: referrals.referral_link_status ENUM causes casting errors with @Enumerated(EnumType.STRING)
-- Solution: Convert to VARCHAR(50) with CHECK constraint for validation
--
-- Author: GYDI Development Team
-- Date: 2026-01-26
-- Issue: PostgreSQL ENUM compatibility with Hibernate JPA
-- ENUM values: 'ACTIVE', 'PAUSED', 'EXPIRED', 'DISABLED'
-- =========================================================

-- Step 1: Drop partial indexes that have WHERE clauses referencing status ENUM
-- These indexes were created in V27 and V31
DROP INDEX IF EXISTS referrals.idx_referral_links_affiliate_active;
DROP INDEX IF EXISTS referrals.idx_referral_links_token;
DROP INDEX IF EXISTS referrals.idx_referral_links_expired;
-- NOTE: idx_referral_links_top_performers was already dropped by CASCADE in V49
-- when total_commission and conversions_count columns were removed

-- Step 2: Drop triggers that use the status column (prevent ALTER TYPE error)
DROP TRIGGER IF EXISTS tr_auto_expire_links_insert ON referrals.referral_links;
DROP TRIGGER IF EXISTS tr_auto_expire_links_update ON referrals.referral_links;

-- Step 3: Drop the function (it may reference the ENUM type)
-- CASCADE ensures all dependent objects are dropped
DROP FUNCTION IF EXISTS referrals.auto_expire_links() CASCADE;

-- Step 4: Remove default value (eliminates ENUM dependency)
-- The column has DEFAULT 'ACTIVE' which depends on the ENUM type
ALTER TABLE referrals.referral_links
    ALTER COLUMN status DROP DEFAULT;

-- Step 4: Convert column from ENUM to VARCHAR
ALTER TABLE referrals.referral_links
    ALTER COLUMN status TYPE VARCHAR(50) USING status::TEXT;

-- Step 5: Drop the ENUM type (now safe)
DROP TYPE IF EXISTS referrals.referral_link_status;

-- Step 4: Add CHECK constraint for validation
ALTER TABLE referrals.referral_links
    ADD CONSTRAINT chk_referral_links_status
    CHECK (status IN ('ACTIVE', 'PAUSED', 'EXPIRED', 'DISABLED'));

-- Step 5: Re-add default value (as VARCHAR literal)
-- Restore the DEFAULT 'ACTIVE' that existed before
ALTER TABLE referrals.referral_links
    ALTER COLUMN status SET DEFAULT 'ACTIVE';

-- Step 6: Add index for query performance (if column is frequently queried)
CREATE INDEX IF NOT EXISTS idx_referral_links_status
    ON referrals.referral_links(status)
    WHERE status IS NOT NULL;

-- Step 7: Recreate function to work with VARCHAR instead of ENUM
CREATE OR REPLACE FUNCTION referrals.auto_expire_links()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.expires_at < CURRENT_TIMESTAMP AND NEW.status = 'ACTIVE' THEN
        NEW.status = 'EXPIRED';
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Step 8: Recreate triggers that were dropped in Step 1
CREATE TRIGGER tr_auto_expire_links_insert
    BEFORE INSERT ON referrals.referral_links
    FOR EACH ROW
    EXECUTE FUNCTION referrals.auto_expire_links();

-- Note: Removed WHEN clause because it references the column type which was just converted
-- The WHEN condition is now handled inside the function itself
CREATE TRIGGER tr_auto_expire_links_update
    BEFORE UPDATE ON referrals.referral_links
    FOR EACH ROW
    EXECUTE FUNCTION referrals.auto_expire_links();

-- Step 9: Recreate partial indexes that were dropped in Step 1
-- Now they work with VARCHAR instead of ENUM
CREATE INDEX idx_referral_links_affiliate_active
ON referrals.referral_links(affiliate_id, created_at DESC)
WHERE status = 'ACTIVE' AND deleted_at IS NULL;

CREATE INDEX idx_referral_links_token
ON referrals.referral_links(encrypted_token)
WHERE status = 'ACTIVE' AND deleted_at IS NULL;

CREATE INDEX idx_referral_links_expired
ON referrals.referral_links(expires_at)
WHERE status = 'ACTIVE' AND deleted_at IS NULL;

-- NOTE: idx_referral_links_top_performers was NOT recreated because:
-- V49 dropped columns total_commission and conversions_count that this index uses
-- The index was automatically dropped by CASCADE in V49

-- =========================================================
-- Rollback Plan:
-- =========================================================
-- DROP TRIGGER IF EXISTS tr_auto_expire_links_update ON referrals.referral_links;
-- DROP TRIGGER IF EXISTS tr_auto_expire_links_insert ON referrals.referral_links;
-- DROP INDEX IF EXISTS idx_referral_links_status;
-- ALTER TABLE referrals.referral_links DROP CONSTRAINT IF EXISTS chk_referral_links_status;
-- CREATE TYPE referrals.referral_link_status AS ENUM ('ACTIVE', 'PAUSED', 'EXPIRED', 'DISABLED');
-- ALTER TABLE referrals.referral_links
--     ALTER COLUMN status TYPE referrals.referral_link_status USING status::referrals.referral_link_status;
-- (Recreate triggers with ENUM)
-- =========================================================

COMMENT ON COLUMN referrals.referral_links.status IS 'Referral link status: ACTIVE, PAUSED, EXPIRED, DISABLED (converted from ENUM to VARCHAR for Hibernate compatibility)';
