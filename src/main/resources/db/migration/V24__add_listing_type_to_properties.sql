-- ========================================
-- Add Listing Type to Properties
-- Version: V24
-- Description: Adds listing_type column to support SHORT_TERM_RENTAL, SALE, or BOTH
-- ========================================

-- Add listing_type column with default SHORT_TERM_RENTAL (backward compatibility)
ALTER TABLE properties.properties
ADD COLUMN listing_type VARCHAR(20) NOT NULL DEFAULT 'SHORT_TERM_RENTAL';

-- Add check constraint for valid values
ALTER TABLE properties.properties
ADD CONSTRAINT chk_listing_type
CHECK (listing_type IN ('SHORT_TERM_RENTAL', 'SALE', 'BOTH'));

-- Create index for filtering by listing type (performance for search)
CREATE INDEX idx_properties_listing_type ON properties.properties(listing_type);

-- Create composite index for common query patterns (property_type + listing_type)
CREATE INDEX idx_properties_type_and_listing ON properties.properties(property_type, listing_type);

-- Add comment for documentation
COMMENT ON COLUMN properties.properties.listing_type IS
'Type of listing: SHORT_TERM_RENTAL (vacation rental), SALE (for sale), or BOTH (available for rental and sale)';
