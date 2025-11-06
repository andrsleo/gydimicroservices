-- ============================================
-- Migration: V1.10 - Password Reset Tokens and Audit
-- Description: Creates tables for password reset functionality
-- Author: GYDI Development Team
-- Date: 2025-10-28
-- ============================================

-- ============================================
-- Table: password_reset_tokens
-- Purpose: Stores password reset tokens for user password recovery
-- ============================================
CREATE TABLE users.password_reset_tokens (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    token VARCHAR(255) NOT NULL UNIQUE,
    expires_at TIMESTAMP NOT NULL,
    used BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    used_at TIMESTAMP NULL,
    ip_address VARCHAR(45) NULL,

    -- Foreign key constraint to users table
    CONSTRAINT fk_password_reset_tokens_user
        FOREIGN KEY (user_id)
        REFERENCES users.users(id)
        ON DELETE CASCADE
);

-- Create indexes for performance optimization
CREATE INDEX idx_password_reset_tokens_token ON users.password_reset_tokens(token);
CREATE INDEX idx_password_reset_tokens_user_id ON users.password_reset_tokens(user_id);
CREATE INDEX idx_password_reset_tokens_expires_at ON users.password_reset_tokens(expires_at);
CREATE INDEX idx_password_reset_tokens_used ON users.password_reset_tokens(used);

-- Add comment to table
COMMENT ON TABLE users.password_reset_tokens IS 'Stores password reset tokens with expiration and usage tracking';
COMMENT ON COLUMN users.password_reset_tokens.token IS 'Unique UUID token sent to user via email';
COMMENT ON COLUMN users.password_reset_tokens.expires_at IS 'Token expiration timestamp (typically 1 hour from creation)';
COMMENT ON COLUMN users.password_reset_tokens.used IS 'Whether the token has been used to reset password';
COMMENT ON COLUMN users.password_reset_tokens.ip_address IS 'IP address from which the reset was requested';

-- ============================================
-- Table: password_reset_audit
-- Purpose: Audit log for all password reset actions
-- ============================================
CREATE TABLE users.password_reset_audit (
    id BIGSERIAL PRIMARY KEY,
    user_email VARCHAR(255) NOT NULL,
    action VARCHAR(50) NOT NULL,
    success BOOLEAN NOT NULL DEFAULT FALSE,
    ip_address VARCHAR(45) NULL,
    user_agent VARCHAR(500) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Add constraint to validate action types
    CONSTRAINT chk_password_reset_audit_action
        CHECK (action IN ('REQUEST', 'VALIDATE', 'RESET', 'EXPIRED', 'INVALID', 'RATE_LIMIT_EXCEEDED'))
);

-- Create indexes for audit queries
CREATE INDEX idx_password_reset_audit_user_email ON users.password_reset_audit(user_email);
CREATE INDEX idx_password_reset_audit_action ON users.password_reset_audit(action);
CREATE INDEX idx_password_reset_audit_created_at ON users.password_reset_audit(created_at);
CREATE INDEX idx_password_reset_audit_email_created ON users.password_reset_audit(user_email, created_at);

-- Add comments to audit table
COMMENT ON TABLE users.password_reset_audit IS 'Audit log for password reset operations with security tracking';
COMMENT ON COLUMN users.password_reset_audit.action IS 'Type of action: REQUEST, VALIDATE, RESET, EXPIRED, INVALID, RATE_LIMIT_EXCEEDED';
COMMENT ON COLUMN users.password_reset_audit.success IS 'Whether the action completed successfully';
COMMENT ON COLUMN users.password_reset_audit.user_agent IS 'Browser/client user agent string for security tracking';

-- ============================================
-- Function: Clean up expired tokens (optional, for manual cleanup)
-- ============================================
CREATE OR REPLACE FUNCTION users.cleanup_expired_password_reset_tokens()
RETURNS INTEGER AS $$
DECLARE
    deleted_count INTEGER;
BEGIN
    DELETE FROM users.password_reset_tokens
    WHERE expires_at < CURRENT_TIMESTAMP - INTERVAL '7 days'
       OR (used = TRUE AND used_at < CURRENT_TIMESTAMP - INTERVAL '30 days');

    GET DIAGNOSTICS deleted_count = ROW_COUNT;
    RETURN deleted_count;
END;
$$ LANGUAGE plpgsql;

COMMENT ON FUNCTION users.cleanup_expired_password_reset_tokens() IS 'Removes expired tokens older than 7 days and used tokens older than 30 days';