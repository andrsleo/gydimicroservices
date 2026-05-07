-- V100: Add sale_price_currency column, rename sale_price → sale_price_amount,
--       relax price_currency constraint from 6-currency enum to regex-based ISO 4217.

-- 1. Rename existing sale_price column to sale_price_amount
ALTER TABLE properties.properties
    RENAME COLUMN sale_price TO sale_price_amount;

-- 2. Increase precision of sale_price_amount to DECIMAL(15, 2)
ALTER TABLE properties.properties
    ALTER COLUMN sale_price_amount TYPE DECIMAL(15, 2);

-- 3. Add sale_price_currency column (nullable — properties without sale have no currency needed)
ALTER TABLE properties.properties
    ADD COLUMN IF NOT EXISTS sale_price_currency VARCHAR(10);

-- 4. Backfill: existing sale_price_amount rows inherit price_currency
UPDATE properties.properties
SET sale_price_currency = price_currency
WHERE sale_price_amount IS NOT NULL
  AND sale_price_currency IS NULL;

-- 5. Drop old hard-coded 6-currency CHECK constraint
ALTER TABLE properties.properties
    DROP CONSTRAINT IF EXISTS chk_currency;

-- 6. Add regex-based currency constraint — accepts any 3-4 uppercase letter ISO 4217 code
ALTER TABLE properties.properties
    ADD CONSTRAINT chk_price_currency_format
    CHECK (price_currency ~ '^[A-Z]{3,4}$');

-- 7. Add constraint for sale_price_currency when present
ALTER TABLE properties.properties
    ADD CONSTRAINT chk_sale_price_currency_format
    CHECK (sale_price_currency IS NULL OR sale_price_currency ~ '^[A-Z]{3,4}$');

-- 8. Consistency constraint: if sale_price_amount is set, currency must be set too
ALTER TABLE properties.properties
    ADD CONSTRAINT chk_sale_price_currency_required
    CHECK (
        (sale_price_amount IS NULL AND sale_price_currency IS NULL) OR
        (sale_price_amount IS NOT NULL AND sale_price_currency IS NOT NULL)
    );

-- 9. Drop old index (references renamed column by name internally, must recreate)
DROP INDEX IF EXISTS properties.idx_properties_sale_price;

-- 10. Recreate index on the renamed column
CREATE INDEX IF NOT EXISTS idx_properties_sale_price_amount
    ON properties.properties(sale_price_amount)
    WHERE sale_price_amount IS NOT NULL;

COMMENT ON COLUMN properties.properties.sale_price_amount IS
    'Sale price amount for SALE or BOTH listing types. Uses sale_price_currency.';

COMMENT ON COLUMN properties.properties.sale_price_currency IS
    'ISO 4217 currency code for sale_price_amount. Must be set when sale_price_amount is not null.';
