-- V100: Add sale_price_currency column, rename sale_price → sale_price_amount,
--       relax price_currency constraint from 6-currency enum to regex-based ISO 4217.

-- 1. Rename existing sale_price column to sale_price_amount
ALTER TABLE properties.properties
    RENAME COLUMN sale_price TO sale_price_amount;

-- 1a. Widen price_currency column from VARCHAR(3) to VARCHAR(10) to match new regex constraint
ALTER TABLE properties.properties
    ALTER COLUMN price_currency TYPE VARCHAR(10);

-- 2. Confirm precision — already NUMERIC(15,2) per V25; this is a no-op kept for auditability
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

-- 4a. Verify backfill completeness before adding NOT-NULL-paired constraints
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM properties.properties
        WHERE sale_price_amount IS NOT NULL AND sale_price_currency IS NULL
    ) THEN
        RAISE EXCEPTION 'V100 backfill incomplete — sale_price_currency is NULL on rows with sale_price_amount';
    END IF;
END $$;

-- 5. Drop old hard-coded 6-currency CHECK constraint
ALTER TABLE properties.properties
    DROP CONSTRAINT IF EXISTS chk_currency;

-- 6. Add regex-based currency constraint — accepts any 3-4 uppercase letter ISO 4217 code
ALTER TABLE properties.properties
    ADD CONSTRAINT chk_price_currency_format
    CHECK (price_currency ~ '^[A-Z]{3,4}$');
-- Note: regex allows 3-4 chars (not strictly ISO 4217's 3) to accommodate future extended codes

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

-- 8a. Drop legacy positive-value constraint from V24 (references renamed column) and re-add with correct name
ALTER TABLE properties.properties
    DROP CONSTRAINT IF EXISTS chk_sale_price_positive;
ALTER TABLE properties.properties
    ADD CONSTRAINT chk_sale_price_amount_positive
    CHECK (sale_price_amount IS NULL OR sale_price_amount > 0);

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
