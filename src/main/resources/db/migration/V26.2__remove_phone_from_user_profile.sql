-- Remove phone_number column from user_profile table
-- The phone_number in users table is the source of truth

ALTER TABLE users.user_profile DROP COLUMN phone_number;
