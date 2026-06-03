-- Migration V105: create_content_schema
-- Date: 2026-05-22

-- ============================================================
-- Creates the content schema for UGC Social Commerce Phase 1.
-- Tables: content_posts, content_media, content_reports
-- ============================================================

CREATE SCHEMA IF NOT EXISTS content;

CREATE TABLE IF NOT EXISTS content.content_posts (
    id BIGSERIAL PRIMARY KEY,
    creator_id BIGINT NOT NULL REFERENCES users.users(id) ON DELETE RESTRICT,
    property_id BIGINT REFERENCES properties.properties(id) ON DELETE SET NULL,
    caption VARCHAR(500),
    type VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    thumbnail_url TEXT,
    view_count INTEGER NOT NULL DEFAULT 0,
    like_count INTEGER NOT NULL DEFAULT 0,
    save_count INTEGER NOT NULL DEFAULT 0,
    comment_count INTEGER NOT NULL DEFAULT 0,
    share_count INTEGER NOT NULL DEFAULT 0,
    engagement_score DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    published_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_content_type CHECK (type IN ('VIDEO', 'PHOTO', 'CAROUSEL')),
    CONSTRAINT chk_content_status CHECK (status IN ('DRAFT', 'PROCESSING', 'PUBLISHED', 'REJECTED', 'ARCHIVED', 'HIDDEN'))
);

CREATE INDEX IF NOT EXISTS idx_content_posts_creator ON content.content_posts(creator_id);
CREATE INDEX IF NOT EXISTS idx_content_posts_property ON content.content_posts(property_id);
CREATE INDEX IF NOT EXISTS idx_content_posts_status ON content.content_posts(status);
CREATE INDEX IF NOT EXISTS idx_content_posts_published ON content.content_posts(published_at DESC)
    WHERE status = 'PUBLISHED';
CREATE INDEX IF NOT EXISTS idx_content_posts_engagement ON content.content_posts(engagement_score DESC)
    WHERE status = 'PUBLISHED';

CREATE TABLE IF NOT EXISTS content.content_media (
    id BIGSERIAL PRIMARY KEY,
    content_post_id BIGINT NOT NULL REFERENCES content.content_posts(id) ON DELETE CASCADE,
    original_url TEXT NOT NULL,
    processed_url TEXT,
    thumbnail_url TEXT,
    media_type VARCHAR(10) NOT NULL,
    display_order INTEGER NOT NULL DEFAULT 0,
    duration_seconds INTEGER,
    width INTEGER,
    height INTEGER,
    file_size_bytes BIGINT,
    processing_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_media_type CHECK (media_type IN ('VIDEO', 'IMAGE')),
    CONSTRAINT chk_processing_status CHECK (processing_status IN ('PENDING', 'PROCESSING', 'READY', 'FAILED'))
);

CREATE TABLE IF NOT EXISTS content.content_reports (
    id BIGSERIAL PRIMARY KEY,
    content_post_id BIGINT NOT NULL REFERENCES content.content_posts(id) ON DELETE RESTRICT,
    reporter_id BIGINT NOT NULL REFERENCES users.users(id) ON DELETE RESTRICT,
    reason VARCHAR(30) NOT NULL,
    description TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    reviewed_by BIGINT REFERENCES users.users(id) ON DELETE SET NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    reviewed_at TIMESTAMP,
    UNIQUE(content_post_id, reporter_id),
    CONSTRAINT chk_report_reason CHECK (reason IN ('SPAM', 'NSFW', 'MISLEADING', 'COPYRIGHT', 'HARASSMENT', 'OTHER')),
    CONSTRAINT chk_report_status CHECK (status IN ('PENDING', 'REVIEWED', 'ACTIONED', 'DISMISSED'))
);
