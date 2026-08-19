-- ============================================
-- Migration: Update roles to USER and ADMIN
-- Description: Simplify role system to USER (unified) and ADMIN
-- Date: 2025-01-XX
-- ============================================

-- 1. Add USER role if it doesn't exist
INSERT INTO users.roles (name, description, display_order)
SELECT 'USER', 'Unified role for all authenticated users (property owners, referrers, renters)', 4
WHERE NOT EXISTS (SELECT 1 FROM users.roles WHERE name = 'USER');

-- 2. Migrate OWNER role assignments to USER
UPDATE users.user_roles
SET role_id = (SELECT id FROM users.roles WHERE name = 'USER')
WHERE role_id IN (SELECT id FROM users.roles WHERE name = 'OWNER');

-- 3. Migrate GUEST role assignments to USER
UPDATE users.user_roles
SET role_id = (SELECT id FROM users.roles WHERE name = 'USER')
WHERE role_id IN (SELECT id FROM users.roles WHERE name = 'GUEST');

-- 4. Deactivate old roles (keep for audit purposes, don't delete)
UPDATE users.roles
SET is_active = false,
    description = CONCAT('[DEPRECATED] ', description)
WHERE name IN ('OWNER', 'GUEST');

-- 5. Update role descriptions
UPDATE users.roles
SET description = 'System administrator with complete access and elevated privileges'
WHERE name = 'ADMIN';

-- 6. Add comment to document migration
COMMENT ON TABLE users.roles IS 'User roles table. System uses USER (unified role) and ADMIN. OWNER and GUEST are deprecated but kept for audit trail.';