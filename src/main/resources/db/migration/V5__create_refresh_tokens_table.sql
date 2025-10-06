-- ============================================
-- Migration: Create refresh_tokens table
-- Version: V5
-- Description: Adds support for JWT refresh tokens
-- ============================================

-- Create refresh_tokens table in users schema
CREATE TABLE users.refresh_tokens (
    id BIGSERIAL PRIMARY KEY,
    token VARCHAR(255) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    expiry_date TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,

    CONSTRAINT fk_refresh_token_user
        FOREIGN KEY (user_id)
        REFERENCES users.users(id)
        ON DELETE CASCADE
);

-- Create indexes for performance
CREATE INDEX idx_refresh_tokens_token ON users.refresh_tokens(token);
CREATE INDEX idx_refresh_tokens_user_id ON users.refresh_tokens(user_id);
CREATE INDEX idx_refresh_tokens_expiry_date ON users.refresh_tokens(expiry_date);

-- Create table for token blacklist (for logout)
CREATE TABLE users.token_blacklist (
    id BIGSERIAL PRIMARY KEY,
    token VARCHAR(500) NOT NULL UNIQUE,
    expiry_date TIMESTAMP NOT NULL,
    blacklisted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Index for efficient token lookup
CREATE INDEX idx_token_blacklist_token ON users.token_blacklist(token);
CREATE INDEX idx_token_blacklist_expiry_date ON users.token_blacklist(expiry_date);

-- Comment the tables
COMMENT ON TABLE users.refresh_tokens IS 'Stores refresh tokens for JWT authentication';
COMMENT ON TABLE users.token_blacklist IS 'Stores blacklisted JWT tokens for logout functionality';

-- Comment the columns
COMMENT ON COLUMN users.refresh_tokens.token IS 'Unique refresh token string (UUID)';
COMMENT ON COLUMN users.refresh_tokens.user_id IS 'Reference to the user who owns this token';
COMMENT ON COLUMN users.refresh_tokens.expiry_date IS 'When this refresh token expires';
COMMENT ON COLUMN users.refresh_tokens.revoked IS 'Whether this token has been revoked (logout all devices)';

COMMENT ON COLUMN users.token_blacklist.token IS 'Blacklisted JWT access token';
COMMENT ON COLUMN users.token_blacklist.expiry_date IS 'Original token expiry date (for cleanup)';