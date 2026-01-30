-- ========================================
-- Fix URL Constraints to Allow Empty Strings
-- Version: V47
-- Description: Updates check constraints to allow empty strings for Airbnb-related URL fields
-- ========================================

-- =============================================
-- Fix 1: chk_ical_url_airbnb_format
-- =============================================
ALTER TABLE properties.properties
    DROP CONSTRAINT IF EXISTS chk_ical_url_airbnb_format;

ALTER TABLE properties.properties
    ADD CONSTRAINT chk_ical_url_airbnb_format CHECK (
        ical_url_airbnb IS NULL OR
        ical_url_airbnb = '' OR
        ical_url_airbnb ~* '^https?://.*$'
    );

-- =============================================
-- Fix 2: chk_airbnb_url_format
-- =============================================
ALTER TABLE properties.properties
    DROP CONSTRAINT IF EXISTS chk_airbnb_url_format;

ALTER TABLE properties.properties
    ADD CONSTRAINT chk_airbnb_url_format CHECK (
        airbnb_url IS NULL OR
        airbnb_url = '' OR
        airbnb_url ~* '^https?://(www\.)?airbnb\.(com|es|mx|ca|co\.uk)/rooms/\d+.*$'
    );

-- =============================================
-- Fix 3: chk_airbnb_listing_id_format
-- =============================================
ALTER TABLE properties.properties
    DROP CONSTRAINT IF EXISTS chk_airbnb_listing_id_format;

ALTER TABLE properties.properties
    ADD CONSTRAINT chk_airbnb_listing_id_format CHECK (
        airbnb_listing_id IS NULL OR
        airbnb_listing_id = '' OR
        airbnb_listing_id ~ '^\d+$'
    );

-- Add comments
COMMENT ON CONSTRAINT chk_ical_url_airbnb_format ON properties.properties
    IS 'Validates iCal URL format: allows NULL, empty string, or valid HTTP(S) URL';

COMMENT ON CONSTRAINT chk_airbnb_url_format ON properties.properties
    IS 'Validates Airbnb URL format: allows NULL, empty string, or valid Airbnb rooms URL';

COMMENT ON CONSTRAINT chk_airbnb_listing_id_format ON properties.properties
    IS 'Validates Airbnb listing ID format: allows NULL, empty string, or numeric ID';
