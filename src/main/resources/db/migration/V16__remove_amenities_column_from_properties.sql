-- V17: Remove deprecated amenities JSONB column from properties table
-- The amenities are now stored in the property_amenities junction table

-- Drop the old amenities column (was JSONB)
ALTER TABLE properties.properties DROP COLUMN IF EXISTS amenities;

-- Add comment
COMMENT ON TABLE properties.properties IS 'Properties table - amenities are now managed via property_amenities junction table';