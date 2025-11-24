-- V22: Make display_order unique constraint deferrable to allow batch updates
-- This allows PostgreSQL to defer constraint checking until transaction commit,
-- preventing violations during intermediate states when reordering images

-- Drop the existing constraint
ALTER TABLE properties.property_images
DROP CONSTRAINT IF EXISTS uq_property_image_order;

-- Recreate the constraint as DEFERRABLE INITIALLY DEFERRED
ALTER TABLE properties.property_images
ADD CONSTRAINT uq_property_image_order
    UNIQUE (property_id, display_order)
    DEFERRABLE INITIALLY DEFERRED;

-- Add comment explaining the constraint
COMMENT ON CONSTRAINT uq_property_image_order ON properties.property_images IS
'Ensures each property has unique display orders for images. Deferrable to allow batch reordering within transactions.';