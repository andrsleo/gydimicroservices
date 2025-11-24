-- V23: Drop the unique index on display_order to allow deferrable constraint to work
-- The unique INDEX created in V21 cannot be deferrable, so we drop it
-- and rely only on the deferrable CONSTRAINT from V22

-- Drop the unique index (indexes cannot be deferrable in PostgreSQL)
DROP INDEX IF EXISTS properties.idx_property_images_unique_order;

-- The constraint uq_property_image_order (from V22) is already deferrable
-- and will handle uniqueness checking at transaction commit time
