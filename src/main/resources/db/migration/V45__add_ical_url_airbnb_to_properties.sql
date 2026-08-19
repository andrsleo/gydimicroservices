-- ========================================
-- Add Airbnb iCal URL to Properties
-- Version: V45
-- Description: Adds ical_url_airbnb field to properties table
-- ========================================

ALTER TABLE properties.properties
    ADD COLUMN ical_url_airbnb VARCHAR(500);

-- Add constraint to validate URL format (optional but recommended)
-- Basic check to ensure it looks like a URL if provided
ALTER TABLE properties.properties
    ADD CONSTRAINT chk_ical_url_airbnb_format CHECK (
        ical_url_airbnb IS NULL OR
        ical_url_airbnb ~* '^https?://.*$'
    );

COMMENT ON COLUMN properties.properties.ical_url_airbnb IS 'URL for Airbnb iCal calendar synchronization';
