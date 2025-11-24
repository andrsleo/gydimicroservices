-- ============================================
-- Migration V38: Backfill slugs for existing properties
-- ============================================
-- Purpose: Generate slugs for properties that don't have them
-- Format: {slug}-{shortId} (e.g., "beach-house-malibu-x7k2m")
-- Author: GYDI Development Team
-- Date: 2025-01-20

-- Function to generate slug from title
CREATE OR REPLACE FUNCTION generate_slug(title TEXT, property_id BIGINT)
RETURNS TEXT AS $$
DECLARE
    slug TEXT;
    short_id TEXT;
BEGIN
    -- Generate short ID from property ID (base 36 to keep it short)
    short_id := LOWER(TO_HEX(property_id));

    -- Clean title: lowercase, remove special characters, replace spaces with hyphens
    slug := LOWER(TRIM(title));
    slug := REGEXP_REPLACE(slug, '[^a-z0-9\s-]', '', 'g');  -- Remove special chars
    slug := REGEXP_REPLACE(slug, '\s+', '-', 'g');          -- Replace spaces with hyphens
    slug := REGEXP_REPLACE(slug, '-+', '-', 'g');           -- Remove consecutive hyphens
    slug := TRIM(BOTH '-' FROM slug);                       -- Remove leading/trailing hyphens

    -- Limit to 50 characters for the title part
    IF LENGTH(slug) > 50 THEN
        slug := SUBSTRING(slug FROM 1 FOR 50);
        slug := TRIM(BOTH '-' FROM slug);
    END IF;

    -- Combine: title-shortid
    RETURN slug || '-' || short_id;
END;
$$ LANGUAGE plpgsql IMMUTABLE;

-- Update all properties that don't have a slug
UPDATE properties.properties
SET slug = generate_slug(title, id)
WHERE slug IS NULL;

-- Make slug NOT NULL now that all properties have slugs
ALTER TABLE properties.properties
ALTER COLUMN slug SET NOT NULL;

-- Drop the temporary function
DROP FUNCTION generate_slug(TEXT, BIGINT);

-- Add comment
COMMENT ON COLUMN properties.properties.slug IS 'SEO-friendly URL slug (required, e.g., beach-house-malibu-x7k2m)';
