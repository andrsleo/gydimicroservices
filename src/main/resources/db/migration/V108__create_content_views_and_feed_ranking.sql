-- ShedLock table (may already exist from CalendarSyncScheduler — use IF NOT EXISTS)
CREATE TABLE IF NOT EXISTS shedlock (
    name VARCHAR(64) NOT NULL,
    lock_until TIMESTAMP NOT NULL,
    locked_at TIMESTAMP NOT NULL,
    locked_by VARCHAR(255) NOT NULL,
    PRIMARY KEY (name)
);

-- Content views tracking
CREATE TABLE IF NOT EXISTS content.content_views (
    id BIGSERIAL PRIMARY KEY,
    content_post_id BIGINT NOT NULL REFERENCES content.content_posts(id) ON DELETE CASCADE,
    viewer_id BIGINT REFERENCES users.users(id),
    viewer_ip VARCHAR(45),
    watch_duration_seconds INTEGER,
    source VARCHAR(20),   -- FEED, PROFILE, PROPERTY, DIRECT, SHARE
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_content_views_post ON content.content_views(content_post_id);
CREATE UNIQUE INDEX idx_content_views_dedup
    ON content.content_views(content_post_id, viewer_id, (created_at::date))
    WHERE viewer_id IS NOT NULL;

-- Feed ranked materialized view
CREATE MATERIALIZED VIEW IF NOT EXISTS content.feed_ranked AS
SELECT
    cp.id,
    cp.creator_id,
    cp.property_id,
    cp.engagement_score,
    cp.published_at,
    (cp.engagement_score * 0.4 +
     EXP(-EXTRACT(EPOCH FROM (NOW() - cp.published_at)) / 86400.0) * 0.6
    ) AS feed_score
FROM content.content_posts cp
WHERE cp.status = 'PUBLISHED'
ORDER BY (cp.engagement_score * 0.4 + EXP(-EXTRACT(EPOCH FROM (NOW() - cp.published_at)) / 86400.0) * 0.6) DESC;

CREATE UNIQUE INDEX IF NOT EXISTS idx_feed_ranked_id ON content.feed_ranked(id);
CREATE INDEX IF NOT EXISTS idx_feed_ranked_score ON content.feed_ranked(feed_score DESC);
