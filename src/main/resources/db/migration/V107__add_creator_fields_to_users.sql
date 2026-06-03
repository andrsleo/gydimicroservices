-- Migration V107: add_creator_fields_to_users
-- Date: 2026-05-22

-- ============================================================
-- Adds creator profile fields to users table and creates
-- creator_profiles table for UGC Social Commerce Phase 1.
-- Inserts CREATOR role into roles table.
-- ============================================================

ALTER TABLE users.users
    ADD COLUMN IF NOT EXISTS bio VARCHAR(160),
    ADD COLUMN IF NOT EXISTS display_name VARCHAR(50),
    ADD COLUMN IF NOT EXISTS cover_image_url TEXT,
    ADD COLUMN IF NOT EXISTS is_verified_creator BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS creator_since TIMESTAMP;

CREATE TABLE IF NOT EXISTS users.creator_profiles (
    user_id BIGINT PRIMARY KEY REFERENCES users.users(id) ON DELETE CASCADE,
    content_count INTEGER NOT NULL DEFAULT 0,
    follower_count INTEGER NOT NULL DEFAULT 0,
    following_count INTEGER NOT NULL DEFAULT 0,
    total_views BIGINT NOT NULL DEFAULT 0,
    avg_engagement_rate DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    tier VARCHAR(20) NOT NULL DEFAULT 'EMERGING',
    last_content_at TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_creator_tier CHECK (tier IN ('EMERGING', 'RISING', 'ESTABLISHED', 'ELITE'))
);

INSERT INTO users.roles (name, display_order)
VALUES ('CREATOR', 5)
ON CONFLICT (name) DO NOTHING;
