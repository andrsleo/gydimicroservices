-- Migration: Add cover_image_id column to properties table
-- Description: Adds cover_image_id field to properties for main/listing image

-- Add cover_image_id column
ALTER TABLE properties.properties
ADD COLUMN cover_image_id UUID;

-- Add foreign key constraint (optional - allows cover image to be deleted without breaking property)
ALTER TABLE properties.properties
ADD CONSTRAINT fk_properties_cover_image
FOREIGN KEY (cover_image_id)
REFERENCES properties.property_images(id)
ON DELETE SET NULL;

-- Create index for performance
CREATE INDEX idx_properties_cover_image_id
ON properties.properties(cover_image_id);

-- Add unique constraint for display_order per property
CREATE UNIQUE INDEX idx_property_images_unique_order
ON properties.property_images(property_id, display_order);

-- Update comment
COMMENT ON COLUMN properties.properties.cover_image_id IS 'Main/cover image displayed in listings';