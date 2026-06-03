package com.affiliate.rentals.gydi.content.application.port;

import java.util.List;
import java.util.Map;

/**
 * Output port for admin analytics queries.
 *
 * <p>Implementations use native SQL to read from materialized views and aggregation
 * tables without going through the domain model layer — analytics data is projection-only
 * and has no invariants to enforce.</p>
 *
 * @author GYDI Development Team
 */
public interface AdminAnalyticsRepositoryPort {

    /**
     * Returns the GMV (Gross Merchandise Value): sum of {@code total_price} for all
     * bookings with {@code status = 'CONFIRMED'}.
     *
     * @return total confirmed bookings revenue, or 0.0 if none exist
     */
    Double getGmv();

    /**
     * Returns the total number of shares across all content posts.
     *
     * @return count of rows in {@code social.shares}
     */
    Long getTotalShares();

    /**
     * Returns the total number of published content posts.
     *
     * @return count of rows in {@code content.content_posts} where status = 'PUBLISHED'
     */
    Long getTotalPublishedPosts();

    /**
     * Returns the count of published posts created in the last {@code days} days.
     *
     * @param days number of days to look back
     * @return count of published posts in that window
     */
    Long getRecentPostCount(int days);

    /**
     * Returns the count of distinct creators who have at least one published post.
     *
     * @return distinct creator_id count
     */
    Integer getTotalActiveCreators();

    /**
     * Returns aggregate total views from {@code content.daily_content_stats}.
     *
     * @return sum of total_views across all days, or 0 if empty
     */
    Long getTotalViewsFromStats();

    // ─── Creator Leaderboard ─────────────────────────────────────────────────

    /**
     * Returns the top {@code limit} creators from the {@code content.creator_leaderboard}
     * materialized view, ordered by total_views DESC.
     *
     * @param limit maximum number of results to return
     * @return list of raw result maps with keys: creator_id, display_name, username,
     *         content_count, total_views, avg_engagement, follower_count
     */
    List<Map<String, Object>> getTopCreators(int limit);

    /**
     * Returns the count of creators who published their first post in the current calendar month.
     *
     * @return new creator count this month
     */
    Integer getNewCreatorsThisMonth();

    /**
     * Returns the count of creators who published at least one post in the last 30 days.
     *
     * @return active creator count in the last 30 days
     */
    Integer getCreatorsActiveLast30Days();

    // ─── Attribution Funnel ──────────────────────────────────────────────────

    /**
     * Returns the count of distinct bookings linked to content via
     * {@code referrals.content_attributions}.
     *
     * @return total bookings attributable to content
     */
    Integer getTotalBookingsFromContent();

    /**
     * Returns the average {@code total_price} of bookings attributed to content.
     *
     * @return average attributed revenue, or 0.0 if no attributions exist
     */
    Double getAvgAttributedRevenue();

    /**
     * Returns the count of published posts that have a non-null {@code property_id}.
     *
     * @return count of property-linked published posts
     */
    Long getTotalContentWithProperty();
}
