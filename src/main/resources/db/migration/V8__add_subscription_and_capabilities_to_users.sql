-- ============================================
-- Migration: Add subscription plan and user capabilities
-- Description: Add fields for subscription-based permissions and capabilities
-- Date: 2025-01-XX
-- ============================================

-- 1. Add subscription plan column
ALTER TABLE users.users
    ADD COLUMN active_plan VARCHAR(20) NOT NULL DEFAULT 'FREE';

-- 2. Add capability flags
ALTER TABLE users.users
    ADD COLUMN can_publish BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN can_refer BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN can_rent BOOLEAN NOT NULL DEFAULT TRUE;

-- 3. Add account verification flag
ALTER TABLE users.users
    ADD COLUMN account_verified BOOLEAN NOT NULL DEFAULT FALSE;

-- 4. Add constraints
ALTER TABLE users.users
    ADD CONSTRAINT chk_active_plan CHECK (active_plan IN ('FREE', 'PRO', 'ELITE'));

-- 5. Create indexes for performance
CREATE INDEX idx_users_active_plan ON users.users(active_plan) WHERE is_active = true;
CREATE INDEX idx_users_account_verified ON users.users(account_verified) WHERE account_verified = true;
CREATE INDEX idx_users_can_publish ON users.users(can_publish) WHERE can_publish = true;

-- 6. Add comments for documentation
COMMENT ON COLUMN users.users.active_plan IS 'User subscription plan: FREE (2% commission, 10 properties, 50 referrals), PRO (5% commission, 50 properties, 200 referrals), ELITE (10% commission, unlimited)';
COMMENT ON COLUMN users.users.can_publish IS 'Flag indicating if user can publish properties. Can be disabled for moderation purposes.';
COMMENT ON COLUMN users.users.can_refer IS 'Flag indicating if user can generate referral links. Can be disabled for moderation purposes.';
COMMENT ON COLUMN users.users.can_rent IS 'Flag indicating if user can rent/book properties. Can be disabled for moderation purposes.';
COMMENT ON COLUMN users.users.account_verified IS 'Flag indicating if user account is verified. Required for publishing properties and generating referrals.';

-- 7. Update existing users to have verified accounts (backward compatibility)
-- IMPORTANT: In production, you may want to keep this as false and require users to verify
UPDATE users.users
SET account_verified = true
WHERE created_at < NOW() - INTERVAL '7 days';  -- Users older than 7 days are auto-verified

-- 8. Add statistics comment
COMMENT ON TABLE users.users IS 'Users table with subscription-based capabilities. Plan determines commission rates and resource limits.';