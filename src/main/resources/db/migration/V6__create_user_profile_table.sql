-- V6__create_user_profile_table.sql
-- Create user_profile table with 1:1 relationship to users table
-- Following modern best practices for performance, scalability, and maintainability

CREATE TABLE user_profile (
    -- Primary Key
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,

    -- Foreign Key (1:1 relationship with users table)
    user_id BIGINT NOT NULL UNIQUE,

    -- Personal Information
    date_of_birth DATE,
    gender VARCHAR(20),
    bio TEXT,

    -- Contact & Location
    country_code VARCHAR(3),
    timezone VARCHAR(50) DEFAULT 'UTC',
    preferred_language VARCHAR(5) DEFAULT 'en',

    -- Profile Media
    avatar_url VARCHAR(500),
    cover_image_url VARCHAR(500),

    -- Social Links (JSONB for flexibility and extensibility)
    social_links JSONB DEFAULT '{}',

    -- User Preferences (JSONB allows adding new preferences without schema changes)
    preferences JSONB DEFAULT '{}',

    -- Privacy & Settings
    profile_visibility VARCHAR(20) DEFAULT 'public' CHECK (profile_visibility IN ('public', 'private', 'connections')),
    email_notifications_enabled BOOLEAN DEFAULT true,
    sms_notifications_enabled BOOLEAN DEFAULT false,

    -- Metadata (extensible field for future needs)
    metadata JSONB DEFAULT '{}',

    -- Audit Fields
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,

    -- Constraints
    CONSTRAINT fk_user_profile_user FOREIGN KEY (user_id)
        REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT chk_date_of_birth CHECK (date_of_birth <= CURRENT_DATE),
    CONSTRAINT chk_gender CHECK (gender IN ('male', 'female', 'non_binary', 'prefer_not_to_say', 'other'))
);

-- Performance Indexes
-- B-tree index on foreign key for fast joins
CREATE INDEX idx_user_profile_user_id ON user_profile(user_id);

-- Partial indexes (only index non-null values to save space)
CREATE INDEX idx_user_profile_country_code ON user_profile(country_code)
    WHERE country_code IS NOT NULL;

CREATE INDEX idx_user_profile_date_of_birth ON user_profile(date_of_birth)
    WHERE date_of_birth IS NOT NULL;

-- Index for time-based queries (DESC for recent profiles first)
CREATE INDEX idx_user_profile_created_at ON user_profile(created_at DESC);

-- GIN indexes for JSONB columns (enables fast queries on JSON data)
CREATE INDEX idx_user_profile_social_links ON user_profile USING GIN (social_links);
CREATE INDEX idx_user_profile_preferences ON user_profile USING GIN (preferences);
CREATE INDEX idx_user_profile_metadata ON user_profile USING GIN (metadata);

-- Trigger function for automatic updated_at timestamp
CREATE OR REPLACE FUNCTION update_user_profile_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Trigger to automatically update updated_at on row modification
CREATE TRIGGER trg_user_profile_updated_at
    BEFORE UPDATE ON user_profile
    FOR EACH ROW
    EXECUTE FUNCTION update_user_profile_updated_at();

-- Comments for documentation
COMMENT ON TABLE user_profile IS 'Extended user profile information with 1:1 relationship to users table';
COMMENT ON COLUMN user_profile.user_id IS 'Foreign key reference to users.id (1:1 relationship)';
COMMENT ON COLUMN user_profile.social_links IS 'JSONB object containing social media links (e.g., {"linkedin": "url", "twitter": "@handle"})';
COMMENT ON COLUMN user_profile.preferences IS 'JSONB object for user preferences (e.g., {"theme": "dark", "notifications": {...}})';
COMMENT ON COLUMN user_profile.metadata IS 'JSONB object for extensible metadata without schema changes';