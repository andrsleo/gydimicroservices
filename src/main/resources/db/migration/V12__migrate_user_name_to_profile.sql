-- V12: Migrate user names from users.name to user_profile.first_name and last_name
-- This migration splits the full name into first and last names

-- Step 1: Migrate existing names from users to user_profile
-- Split name by first space: everything before = first_name, everything after = last_name
UPDATE user_profile up
SET
    first_name = CASE
        WHEN POSITION(' ' IN u.name) > 0 THEN SUBSTRING(u.name FROM 1 FOR POSITION(' ' IN u.name) - 1)
        ELSE u.name  -- If no space, entire name becomes first_name
    END,
    last_name = CASE
        WHEN POSITION(' ' IN u.name) > 0 THEN SUBSTRING(u.name FROM POSITION(' ' IN u.name) + 1)
        ELSE NULL  -- If no space, last_name is NULL
    END,
    updated_at = CURRENT_TIMESTAMP
FROM users u
WHERE up.user_id = u.id
  AND up.first_name IS NULL  -- Only update if not already set
  AND u.name IS NOT NULL
  AND TRIM(u.name) != '';

-- Step 2: Add comment to users.name indicating deprecation
COMMENT ON COLUMN users.name IS 'DEPRECATED: Use user_profile.first_name and last_name instead. This column will be removed in a future version.';

-- Step 3: Verify migration success
DO $$
DECLARE
    users_with_name INTEGER;
    profiles_without_name INTEGER;
BEGIN
    -- Count users with names
    SELECT COUNT(*) INTO users_with_name
    FROM users
    WHERE name IS NOT NULL AND TRIM(name) != '';

    -- Count profiles without first_name for users who have names
    SELECT COUNT(*) INTO profiles_without_name
    FROM user_profile up
    INNER JOIN users u ON up.user_id = u.id
    WHERE u.name IS NOT NULL
      AND TRIM(u.name) != ''
      AND up.first_name IS NULL;

    IF profiles_without_name > 0 THEN
        RAISE EXCEPTION 'Migration incomplete: % profiles still missing names', profiles_without_name;
    END IF;

    RAISE NOTICE 'Migration successful: Migrated % user names to user_profile', users_with_name;
END $$;
