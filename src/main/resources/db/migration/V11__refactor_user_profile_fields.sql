-- V11__refactor_user_profile_fields.sql
-- Migration to refactor user_profile table fields based on updated requirements
-- Removes avatar_url and timezone, renames country_code to country, adds personal info fields

-- Add new columns
ALTER TABLE users.user_profile
    ADD COLUMN IF NOT EXISTS first_name VARCHAR(100),
    ADD COLUMN IF NOT EXISTS last_name VARCHAR(100),
    ADD COLUMN IF NOT EXISTS phone_number VARCHAR(20),
    ADD COLUMN IF NOT EXISTS city VARCHAR(100),
    ADD COLUMN IF NOT EXISTS address VARCHAR(500),
    ADD COLUMN IF NOT EXISTS postal_code VARCHAR(20),
    ADD COLUMN IF NOT EXISTS website_url VARCHAR(500);

-- Rename country_code to country and change type to VARCHAR(100)
-- First, add the new country column
ALTER TABLE users.user_profile
    ADD COLUMN IF NOT EXISTS country VARCHAR(100);

-- Copy data from country_code to country (if there was any data)
UPDATE users.user_profile
SET country = country_code
WHERE country_code IS NOT NULL;

-- Drop the old country_code column
ALTER TABLE users.user_profile
    DROP COLUMN IF EXISTS country_code;

-- Drop avatar_url and timezone columns
ALTER TABLE users.user_profile
    DROP COLUMN IF EXISTS avatar_url,
    DROP COLUMN IF EXISTS timezone;

-- Add check constraints for phone_number format (basic E.164 format)
ALTER TABLE users.user_profile
    ADD CONSTRAINT check_phone_number_format
        CHECK (phone_number IS NULL OR phone_number ~ '^\+?[1-9]\d{1,14}$');

-- Add check constraint for website_url format
ALTER TABLE users.user_profile
    ADD CONSTRAINT check_website_url_format
        CHECK (website_url IS NULL OR website_url ~ '^https?://');

-- Add indexes for frequently queried fields
CREATE INDEX IF NOT EXISTS idx_user_profile_country
    ON users.user_profile(country);

CREATE INDEX IF NOT EXISTS idx_user_profile_city
    ON users.user_profile(city);

-- Add comments for documentation
COMMENT ON COLUMN users.user_profile.first_name IS 'User''s first name';
COMMENT ON COLUMN users.user_profile.last_name IS 'User''s last name';
COMMENT ON COLUMN users.user_profile.phone_number IS 'User''s phone number in E.164 format';
COMMENT ON COLUMN users.user_profile.country IS 'Full country name (e.g., United States, Spain)';
COMMENT ON COLUMN users.user_profile.city IS 'User''s city of residence';
COMMENT ON COLUMN users.user_profile.address IS 'User''s street address';
COMMENT ON COLUMN users.user_profile.postal_code IS 'User''s postal/ZIP code';
COMMENT ON COLUMN users.user_profile.website_url IS 'User''s personal or business website URL';