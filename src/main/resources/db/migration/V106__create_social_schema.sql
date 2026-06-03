-- Migration V106: create_social_schema
-- Date: 2026-05-22

-- ============================================================
-- Creates the social schema for UGC Social Commerce Phase 1.
-- Tables: follows, likes, saves, comments, shares
-- Depends on: V105 (content schema must exist for FK references)
-- ============================================================

CREATE SCHEMA IF NOT EXISTS social;

CREATE TABLE IF NOT EXISTS social.follows (
    id BIGSERIAL PRIMARY KEY,
    follower_id BIGINT NOT NULL REFERENCES users.users(id) ON DELETE CASCADE,
    following_id BIGINT NOT NULL REFERENCES users.users(id) ON DELETE CASCADE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE(follower_id, following_id),
    CHECK(follower_id != following_id)
);
CREATE INDEX IF NOT EXISTS idx_follows_follower ON social.follows(follower_id);
CREATE INDEX IF NOT EXISTS idx_follows_following ON social.follows(following_id);

CREATE TABLE IF NOT EXISTS social.likes (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users.users(id) ON DELETE CASCADE,
    content_post_id BIGINT NOT NULL REFERENCES content.content_posts(id) ON DELETE CASCADE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE(user_id, content_post_id)
);
CREATE INDEX IF NOT EXISTS idx_likes_content ON social.likes(content_post_id);

CREATE TABLE IF NOT EXISTS social.saves (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users.users(id) ON DELETE CASCADE,
    content_post_id BIGINT NOT NULL REFERENCES content.content_posts(id) ON DELETE CASCADE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE(user_id, content_post_id)
);
CREATE INDEX IF NOT EXISTS idx_saves_user ON social.saves(user_id);

CREATE TABLE IF NOT EXISTS social.comments (
    id BIGSERIAL PRIMARY KEY,
    content_post_id BIGINT NOT NULL REFERENCES content.content_posts(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users.users(id) ON DELETE CASCADE,
    parent_comment_id BIGINT REFERENCES social.comments(id) ON DELETE SET NULL,
    body VARCHAR(300) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'VISIBLE',
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_comment_status CHECK (status IN ('VISIBLE', 'HIDDEN', 'DELETED')),
    CONSTRAINT chk_comment_body_length CHECK (length(body) >= 3)
);
CREATE INDEX IF NOT EXISTS idx_comments_content ON social.comments(content_post_id);

CREATE TABLE IF NOT EXISTS social.shares (
    id BIGSERIAL PRIMARY KEY,
    content_post_id BIGINT NOT NULL REFERENCES content.content_posts(id) ON DELETE CASCADE,
    user_id BIGINT REFERENCES users.users(id) ON DELETE SET NULL,
    platform VARCHAR(20) NOT NULL,
    referral_code VARCHAR(20),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_share_platform CHECK (platform IN ('WHATSAPP', 'INSTAGRAM', 'TWITTER', 'FACEBOOK', 'COPY_LINK', 'OTHER'))
);
