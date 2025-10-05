-- Add phone_number column to users table
ALTER TABLE users.users ADD COLUMN phone_number VARCHAR(20);

-- Create index for phone number lookups
CREATE INDEX idx_users_phone_number ON users.users(phone_number) WHERE phone_number IS NOT NULL;
