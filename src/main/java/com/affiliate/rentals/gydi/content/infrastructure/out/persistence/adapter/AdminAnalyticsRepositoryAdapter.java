package com.affiliate.rentals.gydi.content.infrastructure.out.persistence.adapter;

import com.affiliate.rentals.gydi.content.application.port.AdminAnalyticsRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * JDBC-based adapter for admin analytics queries.
 *
 * <p>Uses native SQL to read from materialized views and cross-schema aggregations.
 * All queries are read-only projections; no domain invariants apply here.</p>
 *
 * @author GYDI Development Team
 */
@Component
@RequiredArgsConstructor
public class AdminAnalyticsRepositoryAdapter implements AdminAnalyticsRepositoryPort {

    private final JdbcTemplate jdbcTemplate;

    // ─── Platform Analytics ──────────────────────────────────────────────────

    @Override
    public Double getGmv() {
        Double result = jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(b.total_price), 0) FROM bookings.booking b WHERE b.status = 'CONFIRMED'",
                Double.class);
        return result != null ? result : 0.0;
    }

    @Override
    public Long getTotalShares() {
        Long result = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM social.shares",
                Long.class);
        return result != null ? result : 0L;
    }

    @Override
    public Long getTotalPublishedPosts() {
        Long result = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM content.content_posts WHERE status = 'PUBLISHED'",
                Long.class);
        return result != null ? result : 0L;
    }

    @Override
    public Long getRecentPostCount(int days) {
        Long result = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM content.content_posts " +
                "WHERE status = 'PUBLISHED' AND published_at >= NOW() - INTERVAL '" + days + " days'",
                Long.class);
        return result != null ? result : 0L;
    }

    @Override
    public Integer getTotalActiveCreators() {
        Integer result = jdbcTemplate.queryForObject(
                "SELECT COUNT(DISTINCT creator_id) FROM content.content_posts WHERE status = 'PUBLISHED'",
                Integer.class);
        return result != null ? result : 0;
    }

    @Override
    public Long getTotalViewsFromStats() {
        Long result = jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(total_views), 0) FROM content.daily_content_stats",
                Long.class);
        return result != null ? result : 0L;
    }

    // ─── Creator Leaderboard ─────────────────────────────────────────────────

    @Override
    public List<Map<String, Object>> getTopCreators(int limit) {
        return jdbcTemplate.queryForList(
                "SELECT creator_id, display_name, username, content_count, " +
                "       total_views, avg_engagement, follower_count " +
                "FROM content.creator_leaderboard " +
                "ORDER BY total_views DESC " +
                "LIMIT ?",
                limit);
    }

    @Override
    public Integer getNewCreatorsThisMonth() {
        Integer result = jdbcTemplate.queryForObject(
                "SELECT COUNT(DISTINCT creator_id) " +
                "FROM content.content_posts " +
                "WHERE status = 'PUBLISHED' " +
                "  AND date_trunc('month', published_at) = date_trunc('month', NOW()) " +
                "  AND creator_id NOT IN (" +
                "      SELECT DISTINCT creator_id FROM content.content_posts " +
                "      WHERE status = 'PUBLISHED' AND published_at < date_trunc('month', NOW())" +
                "  )",
                Integer.class);
        return result != null ? result : 0;
    }

    @Override
    public Integer getCreatorsActiveLast30Days() {
        Integer result = jdbcTemplate.queryForObject(
                "SELECT COUNT(DISTINCT creator_id) FROM content.content_posts " +
                "WHERE status = 'PUBLISHED' AND published_at >= NOW() - INTERVAL '30 days'",
                Integer.class);
        return result != null ? result : 0;
    }

    // ─── Attribution Funnel ──────────────────────────────────────────────────

    @Override
    public Integer getTotalBookingsFromContent() {
        Integer result = jdbcTemplate.queryForObject(
                "SELECT COUNT(DISTINCT booking_id) FROM referrals.content_attributions WHERE booking_id IS NOT NULL",
                Integer.class);
        return result != null ? result : 0;
    }

    @Override
    public Double getAvgAttributedRevenue() {
        Double result = jdbcTemplate.queryForObject(
                "SELECT COALESCE(AVG(b.total_price), 0) " +
                "FROM bookings.booking b " +
                "WHERE b.booking_id IN (SELECT DISTINCT booking_id FROM referrals.content_attributions WHERE booking_id IS NOT NULL)",
                Double.class);
        return result != null ? result : 0.0;
    }

    @Override
    public Long getTotalContentWithProperty() {
        Long result = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM content.content_posts WHERE status = 'PUBLISHED' AND property_id IS NOT NULL",
                Long.class);
        return result != null ? result : 0L;
    }
}
