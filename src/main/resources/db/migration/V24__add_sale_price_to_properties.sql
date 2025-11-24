-- V25__add_sale_price_to_properties.sql
-- Add sale_price column to properties table for properties listed for sale

-- Add sale_price column (nullable to maintain backward compatibility)
ALTER TABLE properties.properties
ADD COLUMN sale_price NUMERIC(10, 2);

-- Add check constraint to ensure sale_price is positive when set
ALTER TABLE properties.properties
ADD CONSTRAINT chk_sale_price_positive
CHECK (sale_price IS NULL OR sale_price > 0);

-- Add comment to document the field
COMMENT ON COLUMN properties.properties.sale_price IS 'Sale price for properties available for sale (used when listing_type is SALE or BOTH)';

-- Create index for queries filtering by sale price ranges
CREATE INDEX idx_properties_sale_price ON properties.properties(sale_price)
WHERE sale_price IS NOT NULL;
