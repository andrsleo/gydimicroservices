-- V26__increase_price_limits.sql
-- Increase price column precision to support higher values (e.g., Colombian Pesos)

-- Modify price_amount column to support larger values
ALTER TABLE properties.properties
ALTER COLUMN price_amount TYPE NUMERIC(15, 2);

-- Modify sale_price column to support larger values
ALTER TABLE properties.properties
ALTER COLUMN sale_price TYPE NUMERIC(15, 2);

-- Add comment explaining the change
COMMENT ON COLUMN properties.properties.price_amount IS 'Price per night - supports up to 999,999,999,999.99 (15 digits, 2 decimals)';
COMMENT ON COLUMN properties.properties.sale_price IS 'Sale price - supports up to 999,999,999,999.99 (15 digits, 2 decimals)';
