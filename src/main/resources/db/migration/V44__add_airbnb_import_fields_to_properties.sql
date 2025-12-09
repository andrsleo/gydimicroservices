-- ========================================
-- Add Airbnb Import Fields to Properties
-- Version: V44
-- Description: Adds fields to support Airbnb data import functionality
-- ========================================

-- Create ENUM type for import mode
CREATE TYPE properties.import_mode AS ENUM ('MANUAL', 'SEMI_IMPORT');

-- Add Airbnb-related columns to properties table
ALTER TABLE properties.properties
    ADD COLUMN airbnb_url VARCHAR(500),
    ADD COLUMN import_mode properties.import_mode DEFAULT 'MANUAL',
    ADD COLUMN imported_at TIMESTAMP,
    ADD COLUMN airbnb_listing_id VARCHAR(100);

-- Add constraints
ALTER TABLE properties.properties
    ADD CONSTRAINT chk_airbnb_url_format CHECK (
        airbnb_url IS NULL OR
        airbnb_url ~* '^https?://(www\.)?airbnb\.(com|es|mx|ca|co\.uk)/rooms/\d+.*$'
    ),
    ADD CONSTRAINT chk_airbnb_listing_id_format CHECK (
        airbnb_listing_id IS NULL OR
        airbnb_listing_id ~ '^\d+$'
    );

-- Create unique index on airbnb_listing_id (only when not null)
-- This prevents importing the same Airbnb listing multiple times
CREATE UNIQUE INDEX idx_properties_airbnb_listing_id
    ON properties.properties(airbnb_listing_id)
    WHERE airbnb_listing_id IS NOT NULL;

-- Create index on airbnb_url for faster lookups
CREATE INDEX idx_properties_airbnb_url
    ON properties.properties(airbnb_url)
    WHERE airbnb_url IS NOT NULL;

-- Create index on import_mode for filtering
CREATE INDEX idx_properties_import_mode
    ON properties.properties(import_mode);

-- Create index on imported_at for sorting/filtering
CREATE INDEX idx_properties_imported_at
    ON properties.properties(imported_at DESC)
    WHERE imported_at IS NOT NULL;

-- Add comments for documentation
COMMENT ON COLUMN properties.properties.airbnb_url IS 'URL of the property listing on Airbnb';
COMMENT ON COLUMN properties.properties.import_mode IS 'How the property data was created: MANUAL (user entered) or SEMI_IMPORT (imported from Airbnb)';
COMMENT ON COLUMN properties.properties.imported_at IS 'Timestamp when data was imported from Airbnb (null if manually created)';
COMMENT ON COLUMN properties.properties.airbnb_listing_id IS 'Airbnb listing ID extracted from the URL (unique across all properties)';

-- Add validation trigger to ensure consistency
CREATE OR REPLACE FUNCTION properties.validate_airbnb_import_fields()
RETURNS TRIGGER AS $$
BEGIN
    -- If import_mode is SEMI_IMPORT, require airbnb_url and airbnb_listing_id
    IF NEW.import_mode = 'SEMI_IMPORT' THEN
        IF NEW.airbnb_url IS NULL OR NEW.airbnb_listing_id IS NULL THEN
            RAISE EXCEPTION 'Properties with import_mode SEMI_IMPORT must have airbnb_url and airbnb_listing_id';
        END IF;

        -- Automatically set imported_at if not provided
        IF NEW.imported_at IS NULL THEN
            NEW.imported_at = CURRENT_TIMESTAMP;
        END IF;
    END IF;

    -- If airbnb_url is provided, extract and validate listing_id
    IF NEW.airbnb_url IS NOT NULL AND NEW.airbnb_listing_id IS NULL THEN
        RAISE EXCEPTION 'If airbnb_url is provided, airbnb_listing_id must also be provided';
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_validate_airbnb_import_fields
BEFORE INSERT OR UPDATE ON properties.properties
FOR EACH ROW
EXECUTE FUNCTION properties.validate_airbnb_import_fields();
