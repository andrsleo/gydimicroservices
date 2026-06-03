-- V112: Create analytics materialized views for Admin Analytics (Phase 5 Track B)
-- Date: 2026-05-25
--
-- Creates two materialized views:
--   1. content.daily_content_stats  — daily aggregates of published content
--   2. content.creator_leaderboard  — creator ranking by total views
--
-- Both views are refreshed hourly via FeedRefreshScheduler (CONCURRENTLY requires
-- a UNIQUE index on the view, created below).
-- Flyway disabled in test profile (application-test.yml) — H2 cannot run these DDL statements.

CREATE MATERIALIZED VIEW IF NOT EXISTS content.daily_content_stats AS
SELECT
    date_trunc('day', cp.published_at) AS day,
    COUNT(*)                            AS posts_published,
    SUM(cp.view_count)                  AS total_views,
    SUM(cp.like_count)                  AS total_likes,
    AVG(cp.engagement_score)            AS avg_engagement,
    COUNT(DISTINCT cp.creator_id)       AS active_creators
FROM content.content_posts cp
WHERE cp.status = 'PUBLISHED'
GROUP BY date_trunc('day', cp.published_at);

CREATE UNIQUE INDEX IF NOT EXISTS idx_daily_content_stats_day
    ON content.daily_content_stats(day);

-- ─────────────────────────────────────────────────────────────────────────────

CREATE MATERIALIZED VIEW IF NOT EXISTS content.creator_leaderboard AS
SELECT
    u.id                                                                AS creator_id,
    CONCAT(up.first_name, ' ', up.last_name)                           AS display_name,
    u.email                                                             AS creator_email,
    COUNT(cp.id)                                                        AS content_count,
    COALESCE(SUM(cp.view_count), 0)                                    AS total_views,
    COALESCE(AVG(cp.engagement_score), 0.0)                            AS avg_engagement,
    COALESCE(f.follower_count, 0)                                      AS follower_count
FROM users.users u
LEFT JOIN users.user_profile up ON u.id = up.user_id
JOIN content.content_posts cp
    ON u.id = cp.creator_id AND cp.status = 'PUBLISHED'
LEFT JOIN (
    SELECT following_id, COUNT(*) AS follower_count
    FROM social.follows
    GROUP BY following_id
) f ON u.id = f.following_id
GROUP BY u.id, up.first_name, up.last_name, u.email, f.follower_count
ORDER BY total_views DESC;

CREATE UNIQUE INDEX IF NOT EXISTS idx_creator_leaderboard_id
    ON content.creator_leaderboard(creator_id);
