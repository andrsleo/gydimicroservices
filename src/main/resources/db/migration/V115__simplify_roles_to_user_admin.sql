-- V115: Simplify role system to USER and ADMIN only
-- The platform uses two system roles:
--   USER  — any authenticated user (can be host, affiliate, or creator based on profile flags)
--   ADMIN — full administrative access
--
-- CREATOR role is replaced by the is_verified_creator boolean flag on users.users.
-- HOST and AFFILIATE are not system roles — they are contextual identities determined
-- by whether the user has properties (host) or referral links (affiliate).
--
-- This mirrors the pattern used in V7 for OWNER and GUEST deprecation.

-- 1. Migrate any existing CREATOR role assignments back to USER
--    Step 1a: Delete CREATOR row for users who already have USER role (avoids PK duplicate)
DELETE FROM users.user_roles
WHERE role_id = (SELECT id FROM users.roles WHERE name = 'CREATOR')
  AND user_id IN (
      SELECT user_id FROM users.user_roles
      WHERE role_id = (SELECT id FROM users.roles WHERE name = 'USER')
  );

--    Step 1b: Migrate remaining CREATOR-only users to USER
UPDATE users.user_roles
SET role_id = (SELECT id FROM users.roles WHERE name = 'USER')
WHERE role_id = (SELECT id FROM users.roles WHERE name = 'CREATOR');

-- 2. Deprecate CREATOR role (keep row for audit trail, same as OWNER/GUEST in V7)
UPDATE users.roles
SET is_active      = false,
    description    = '[DEPRECATED] Use is_verified_creator flag on users table instead.'
WHERE name = 'CREATOR';

-- 3. Update table comment to reflect current design
COMMENT ON TABLE users.roles IS
    'User roles table. Active: USER, ADMIN. '
    'Deprecated: OWNER, GUEST, CREATOR (creator identity = is_verified_creator flag). '
    'Host/affiliate identity is derived from profile data, not system roles.';
