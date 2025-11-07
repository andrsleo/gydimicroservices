-- V27__add_cop_currency.sql
-- Add Colombian Peso (COP) to currency constraint

-- Drop existing currency constraint
ALTER TABLE properties.properties
DROP CONSTRAINT IF EXISTS chk_currency;

-- Add new constraint with COP included
ALTER TABLE properties.properties
ADD CONSTRAINT chk_currency CHECK (price_currency IN ('USD', 'EUR', 'MXN', 'COP', 'CAD', 'GBP'));
