-- ============================================
-- Migration: V47__create_system_user_for_organic_traffic.sql
-- Description: Creates a SYSTEM user for tracking organic traffic
--              (users who arrive without a referral link)
-- ============================================

-- Insert SYSTEM user for organic traffic tracking
-- This user will be used to create referral links for properties when
-- no specific affiliate referral link is used
INSERT INTO users.users (
    email,
    password_hash,
    is_active,
    email_verified,
    created_at,
    updated_at
) VALUES (
    'system-organic@gydi.internal',
    -- Random bcrypt hash (this account cannot be used for login)
    '$2a$10$XQqVZ8kVzKxZ9pJ8eK7Z4eF2jK5vM8qN7wR3tY6uI9oP0qA1sB2cD',
    true,
    true,
    NOW(),
    NOW()
) ON CONFLICT (email) DO NOTHING;

-- Optionally create a user profile for better identification
INSERT INTO users.user_profile (
    user_id,
    first_name,
    last_name,
    created_at,
    updated_at
)
SELECT
    id,
    'SYSTEM',
    'Organic Traffic',
    NOW(),
    NOW()
FROM users.users
WHERE email = 'system-organic@gydi.internal'
ON CONFLICT (user_id) DO NOTHING;

-- Store the SYSTEM user ID for reference (for application use)
-- This is useful for creating referral links programmatically
COMMENT ON TABLE users.users IS 'System user for organic traffic: email=system-organic@gydi.internal';

-- ============================================
-- Notes:
-- ============================================
-- The SYSTEM user will be used to:
-- 1. Track bookings that come without a referral link (organic traffic)
-- 2. Create system-level referral links for properties
-- 3. Differentiate between affiliate-driven vs organic conversions
-- 4. Provide analytics on organic vs referred traffic
--
-- When a user accesses a property without a ?ref= parameter,
-- the backend will automatically create or use a referral link
-- associated with this SYSTEM user.
-- ============================================
