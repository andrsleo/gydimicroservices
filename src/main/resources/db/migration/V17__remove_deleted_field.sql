-- V18: Remove redundant 'deleted' field from properties table
-- Reason: The field is redundant with status = 'DELETED'
-- Using status enum is clearer and avoids potential inconsistencies

-- Drop the deleted column
ALTER TABLE properties.properties DROP COLUMN IF EXISTS deleted;

-- Update comment on status column to clarify it includes soft delete
COMMENT ON COLUMN properties.properties.status IS 'Property lifecycle status: DRAFT, PUBLISHED, INACTIVE, or DELETED (soft delete)';