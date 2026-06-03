package com.affiliate.rentals.gydi.content.infrastructure.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled job that refreshes the {@code content.feed_ranked} materialized view
 * every 5 minutes using ShedLock to prevent concurrent refreshes across instances.
 *
 * <p>{@code REFRESH MATERIALIZED VIEW CONCURRENTLY} requires a UNIQUE index on the view
 * (provided by {@code idx_feed_ranked_id} created in migration V108), allowing reads
 * to continue while the refresh runs.</p>
 *
 * <p>Excluded from test profile — H2 does not support PostgreSQL materialized views.</p>
 *
 * @author GYDI Development Team
 */
@Slf4j
@Component
@Profile("!test")
@RequiredArgsConstructor
public class FeedRefreshScheduler {

    private final JdbcTemplate jdbcTemplate;

    @Scheduled(fixedDelay = 5 * 60 * 1000) // every 5 minutes
    @SchedulerLock(name = "FeedRefreshScheduler", lockAtMostFor = "4m", lockAtLeastFor = "1m")
    public void refreshFeedRankedView() {
        log.info("[FeedRefreshScheduler] Refreshing feed_ranked materialized view");
        jdbcTemplate.execute("REFRESH MATERIALIZED VIEW CONCURRENTLY content.feed_ranked");
        log.info("[FeedRefreshScheduler] feed_ranked refreshed successfully");
    }

    /**
     * Refreshes analytics materialized views every hour.
     *
     * <p>Refreshes {@code content.daily_content_stats} and {@code content.creator_leaderboard}.
     * Both views have UNIQUE indexes (created in V112), enabling CONCURRENT refresh
     * so reads continue uninterrupted during the operation.</p>
     *
     * <p>ShedLock prevents concurrent execution across multiple application instances.</p>
     */
    @Scheduled(fixedDelay = 60 * 60 * 1000) // every hour
    @SchedulerLock(name = "AnalyticsRefreshScheduler", lockAtMostFor = "50m", lockAtLeastFor = "5m")
    public void refreshAnalyticsViews() {
        log.info("[FeedRefreshScheduler] Refreshing analytics materialized views");
        jdbcTemplate.execute("REFRESH MATERIALIZED VIEW CONCURRENTLY content.daily_content_stats");
        jdbcTemplate.execute("REFRESH MATERIALIZED VIEW CONCURRENTLY content.creator_leaderboard");
        log.info("[FeedRefreshScheduler] Analytics views refreshed successfully");
    }
}
