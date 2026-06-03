package com.affiliate.rentals.gydi.content.application.usecase;

import com.affiliate.rentals.gydi.content.application.dto.PlatformAnalyticsDto;
import com.affiliate.rentals.gydi.content.application.port.AdminAnalyticsRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Use case: retrieve platform-wide analytics for the Admin dashboard.
 *
 * <p>Computes the following metrics via native SQL queries:
 * <ul>
 *   <li><b>GMV</b> — sum of {@code total_price} for CONFIRMED bookings.</li>
 *   <li><b>viralCoefficient</b> — total shares ÷ total published posts (0.0 if no posts).</li>
 *   <li><b>contentVelocity</b> — average new published posts per day over the last 7 days.</li>
 *   <li><b>totalCreators</b> — distinct creators with at least one published post.</li>
 *   <li><b>totalViews</b> — aggregate from {@code content.daily_content_stats}.</li>
 *   <li><b>totalPosts</b> — count of all published posts.</li>
 * </ul>
 * </p>
 *
 * @author GYDI Development Team
 */
@Service
@RequiredArgsConstructor
public class GetPlatformAnalyticsUseCase {

    private static final int VELOCITY_WINDOW_DAYS = 7;

    private final AdminAnalyticsRepositoryPort analyticsRepository;

    @Transactional(readOnly = true)
    public PlatformAnalyticsDto execute() {
        Double gmv = analyticsRepository.getGmv();

        Long totalShares = analyticsRepository.getTotalShares();
        Long totalPosts  = analyticsRepository.getTotalPublishedPosts();
        Double viralCoefficient = totalPosts > 0
                ? (double) totalShares / totalPosts
                : 0.0;

        Long recentPosts = analyticsRepository.getRecentPostCount(VELOCITY_WINDOW_DAYS);
        double contentVelocity = (double) recentPosts / VELOCITY_WINDOW_DAYS;

        Integer totalCreators = analyticsRepository.getTotalActiveCreators();
        Long    totalViews    = analyticsRepository.getTotalViewsFromStats();

        return new PlatformAnalyticsDto(
                gmv,
                viralCoefficient,
                contentVelocity,
                totalCreators,
                totalViews,
                totalPosts
        );
    }
}
