-- =========================================================
-- Migration V66: Convert properties.import_mode from ENUM to VARCHAR
-- =========================================================
-- Purpose: Fix PostgreSQL ENUM type mismatch error with Hibernate
-- Context: properties.import_mode ENUM causes casting errors with @Enumerated(EnumType.STRING)
-- Solution: Convert to VARCHAR(50) with CHECK constraint for validation
--
-- Author: GYDI Development Team
-- Date: 2026-01-26
-- Issue: Column "import_mode" is of type properties.import_mode but expression is of type character varying
-- ENUM values: 'MANUAL', 'SEMI_IMPORT'
-- =========================================================

-- Step 1: Remove default value (it depends on the ENUM type)
-- This allows us to drop the ENUM type later
ALTER TABLE properties.properties
    ALTER COLUMN import_mode DROP DEFAULT;

-- Step 2: Convert column from ENUM to VARCHAR
-- Using ALTER TYPE ... USING to safely convert existing values
ALTER TABLE properties.properties
    ALTER COLUMN import_mode TYPE VARCHAR(50) USING import_mode::TEXT;

-- Step 3: Drop the ENUM type (now safe because no dependencies)
DROP TYPE IF EXISTS properties.import_mode;

-- Step 4: Add CHECK constraint for value validation
-- This provides the same validation as the ENUM but compatible with Hibernate
ALTER TABLE properties.properties
    ADD CONSTRAINT chk_import_mode
    CHECK (import_mode IN ('MANUAL', 'SEMI_IMPORT'));

-- Step 5: Re-add default value (now as VARCHAR literal)
-- This maintains the same behavior as before
ALTER TABLE properties.properties
    ALTER COLUMN import_mode SET DEFAULT 'MANUAL';

-- Step 6: Add index on import_mode for query performance (optional but recommended)
CREATE INDEX IF NOT EXISTS idx_properties_import_mode
    ON properties.properties(import_mode)
    WHERE import_mode IS NOT NULL;

-- =========================================================
-- Rollback Plan (if needed):
-- =========================================================
-- To rollback this migration, execute:
--
-- DROP INDEX IF EXISTS idx_properties_import_mode;
-- ALTER TABLE properties.properties ALTER COLUMN import_mode DROP DEFAULT;
-- ALTER TABLE properties.properties DROP CONSTRAINT IF EXISTS chk_import_mode;
-- CREATE TYPE properties.import_mode AS ENUM ('MANUAL', 'SEMI_IMPORT');
-- ALTER TABLE properties.properties
--     ALTER COLUMN import_mode TYPE properties.import_mode USING import_mode::properties.import_mode;
-- ALTER TABLE properties.properties
--     ALTER COLUMN import_mode SET DEFAULT 'MANUAL'::properties.import_mode;
-- =========================================================

COMMENT ON COLUMN properties.properties.import_mode IS 'Property import method (converted from ENUM to VARCHAR for Hibernate compatibility): MANUAL=entered by user, SEMI_IMPORT=imported from Airbnb';
