-- Fix chk_airbnb_url_format to accept any Airbnb regional domain
-- e.g. airbnb.com, airbnb.es, airbnb.com.co, airbnb.com.ar, airbnb.co.uk, etc.
ALTER TABLE properties.properties DROP CONSTRAINT IF EXISTS chk_airbnb_url_format;

ALTER TABLE properties.properties ADD CONSTRAINT chk_airbnb_url_format CHECK (
    airbnb_url IS NULL OR
    airbnb_url = '' OR
    airbnb_url ~* '^https?://(www\.)?airbnb\.[a-z]{2,3}(\.[a-z]{2})?/rooms/[0-9]+.*$'
);
