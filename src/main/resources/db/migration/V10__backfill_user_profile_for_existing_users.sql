-- V10__backfill_user_profile_for_existing_users.sql
-- Backfills user_profile records for all existing users that don't have one yet
-- This migration ensures data integrity after implementing auto-creation in CreateUserUseCase

-- Insert default user profiles for all users that don't have one
-- Uses INSERT ... ON CONFLICT to be idempotent (safe to run multiple times)
INSERT INTO user_profile (
    id,
    user_id,
    timezone,
    preferred_language,
    profile_visibility,
    email_notifications_enabled,
    sms_notifications_enabled,
    social_links,
    preferences,
    metadata,
    created_at,
    updated_at
)
SELECT
    gen_random_uuid() AS id,
    u.id AS user_id,
    'UTC' AS timezone,
    'en' AS preferred_language,
    'public' AS profile_visibility,
    true AS email_notifications_enabled,
    false AS sms_notifications_enabled,
    '{}'::jsonb AS social_links,
    '{}'::jsonb AS preferences,
    '{}'::jsonb AS metadata,
    COALESCE(u.created_at, CURRENT_TIMESTAMP) AS created_at,
    COALESCE(u.updated_at, CURRENT_TIMESTAMP) AS updated_at
FROM users u
WHERE NOT EXISTS (
    SELECT 1
    FROM user_profile up
    WHERE up.user_id = u.id
);

-- Verify that all users now have profiles
DO $$
DECLARE
    users_without_profiles INTEGER;
BEGIN
    SELECT COUNT(*)
    INTO users_without_profiles
    FROM users u
    WHERE NOT EXISTS (
        SELECT 1
        FROM user_profile up
        WHERE up.user_id = u.id
    );

    IF users_without_profiles > 0 THEN
        RAISE EXCEPTION 'Migration failed: % users still without profiles', users_without_profiles;
    END IF;

    RAISE NOTICE 'Migration successful: All users now have user_profile records';
END $$;

-- Add comment for documentation
COMMENT ON COLUMN user_profile.user_id IS 'Foreign key to users.id - backfilled by V10 migration for existing users';