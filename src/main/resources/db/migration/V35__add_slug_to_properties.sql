-- ============================================
-- Migration V10: Add slug column to properties
-- ============================================
-- Purpose: Add SEO-friendly URL slugs to properties for better URLs
-- Format: {slug}-{shortId} (e.g., "beach-house-malibu-x7k2m")
-- Author: GYDI Development Team
-- Date: 2025-01-13

-- Add slug column to properties table
ALTER TABLE properties.properties
ADD COLUMN slug VARCHAR(255);

-- Add unique index on slug for fast lookups
CREATE UNIQUE INDEX idx_properties_slug ON properties.properties(slug) WHERE slug IS NOT NULL;

-- Add comment to column
COMMENT ON COLUMN properties.properties.slug IS 'SEO-friendly URL slug (e.g., beach-house-malibu-x7k2m)';

-- Note: Existing properties will have NULL slugs initially
-- They will be generated via application code or manual migration script