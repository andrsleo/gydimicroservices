-- ============================================
-- Migration: Add refresh token rotation support
-- Version: V62
-- Description: Adds one-time use tokens with rotation tracking following OWASP best practices
-- Security: Implements automatic token family revocation on reuse detection
-- ============================================

-- Add new columns to refresh_tokens table for rotation support
ALTER TABLE users.refresh_tokens
    ADD COLUMN token_family_id UUID NOT NULL DEFAULT gen_random_uuid(),
    ADD COLUMN used_at TIMESTAMP DEFAULT NULL,
    ADD COLUMN replaced_by_token VARCHAR(255) DEFAULT NULL,
    ADD COLUMN revoked_at TIMESTAMP DEFAULT NULL;

-- Rename existing 'revoked' boolean to maintain backward compatibility during transition
-- We'll use revoked_at timestamp going forward for better auditability
ALTER TABLE users.refresh_tokens
    DROP COLUMN revoked;

-- Create index for token family queries (for detecting reuse and revoking families)
CREATE INDEX idx_refresh_tokens_token_family_id ON users.refresh_tokens(token_family_id);

-- Create index for token lookup during rotation
CREATE INDEX idx_refresh_tokens_replaced_by_token ON users.refresh_tokens(replaced_by_token);

-- Add comments for new columns
COMMENT ON COLUMN users.refresh_tokens.token_family_id IS 'UUID shared by all tokens in a rotation chain. Used for detecting token reuse and revoking entire families.';
COMMENT ON COLUMN users.refresh_tokens.used_at IS 'Timestamp when token was used for rotation. If not null, token is invalid (one-time use only).';
COMMENT ON COLUMN users.refresh_tokens.replaced_by_token IS 'Reference to the new token that replaced this one during rotation.';
COMMENT ON COLUMN users.refresh_tokens.revoked_at IS 'Timestamp when token was revoked. If not null, token and potentially entire family is invalid.';

-- Update table comment
COMMENT ON TABLE users.refresh_tokens IS 'Refresh tokens with one-time use and automatic rotation. Implements OWASP token reuse detection.';
