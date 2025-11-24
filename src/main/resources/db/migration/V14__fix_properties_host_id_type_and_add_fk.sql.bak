-- ============================================================================
-- Migration: V15 - Fix properties.host_id type and add foreign key constraint
-- Author: GYDI Development Team
-- Date: 2025-10-30
-- Description:
--   - Changes properties.host_id from UUID to BIGINT to match users.users.id
--   - Adds foreign key constraint for referential integrity
--   - Ensures one-to-many relationship (User -> Properties)
-- ============================================================================

-- Step 1: Drop existing index on host_id (will be recreated after type change)
DROP INDEX IF EXISTS properties.idx_properties_host_id;

-- Step 2: Truncate properties table if it contains data with UUIDs
-- (Safe during development; adjust for production)
TRUNCATE TABLE properties.property_videos CASCADE;
TRUNCATE TABLE properties.property_images CASCADE;
TRUNCATE TABLE properties.properties CASCADE;

-- Step 3: Change host_id column type from UUID to BIGINT
ALTER TABLE properties.properties
    ALTER COLUMN host_id TYPE BIGINT USING NULL;

-- Step 4: Add foreign key constraint to users.users(id)
ALTER TABLE properties.properties
    ADD CONSTRAINT fk_properties_host
    FOREIGN KEY (host_id)
    REFERENCES users.users(id)
    ON DELETE CASCADE
    ON UPDATE CASCADE;

-- Step 5: Recreate index on host_id for query performance
CREATE INDEX idx_properties_host_id ON properties.properties(host_id);

-- Step 6: Add constraint comment for documentation
COMMENT ON CONSTRAINT fk_properties_host ON properties.properties
    IS 'Foreign key to users.users ensuring referential integrity. ON DELETE CASCADE removes properties when host user is deleted.';

-- Step 7: Add column comment
COMMENT ON COLUMN properties.properties.host_id
    IS 'ID of the user who owns/hosts this property. References users.users(id).';
