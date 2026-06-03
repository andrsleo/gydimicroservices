package com.affiliate.rentals.gydi.content.application.usecase;

import com.affiliate.rentals.gydi.content.application.dto.CreatorAnalyticsSummaryDto;
import com.affiliate.rentals.gydi.content.application.dto.CreatorLeaderboardDto;
import com.affiliate.rentals.gydi.content.application.port.AdminAnalyticsRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * Use case: retrieve creator analytics for the Admin dashboard.
 *
 * <p>Reads from the {@code content.creator_leaderboard} materialized view (refreshed hourly).
 * Returns the top 20 creators by total views, plus derived metrics:
 * new creators this month and 30-day retention rate.</p>
 *
 * @author GYDI Development Team
 */
@Service
@RequiredArgsConstructor
public class GetCreatorLeaderboardUseCase {

    private static final int TOP_CREATORS_LIMIT = 20;

    private final AdminAnalyticsRepositoryPort analyticsRepository;

    @Transactional(readOnly = true)
    public CreatorAnalyticsSummaryDto execute() {
        List<Map<String, Object>> rows = analyticsRepository.getTopCreators(TOP_CREATORS_LIMIT);

        List<CreatorLeaderboardDto> topCreators = rows.stream()
                .map(this::mapRow)
                .toList();

        Integer newCreatorsThisMonth = analyticsRepository.getNewCreatorsThisMonth();
        Integer totalCreators        = analyticsRepository.getTotalActiveCreators();
        Integer activeLast30Days     = analyticsRepository.getCreatorsActiveLast30Days();

        double retentionRate = totalCreators > 0
                ? (double) activeLast30Days / totalCreators
                : 0.0;

        return new CreatorAnalyticsSummaryDto(topCreators, newCreatorsThisMonth, retentionRate);
    }

    private CreatorLeaderboardDto mapRow(Map<String, Object> row) {
        return new CreatorLeaderboardDto(
                toLong(row.get("creator_id")),
                (String) row.get("display_name"),
                (String) row.get("username"),
                toInteger(row.get("content_count")),
                toLong(row.get("total_views")),
                toDouble(row.get("avg_engagement")),
                toLong(row.get("follower_count"))
        );
    }

    private Long toLong(Object value) {
        if (value == null) return 0L;
        if (value instanceof Long l) return l;
        if (value instanceof Number n) return n.longValue();
        return 0L;
    }

    private Integer toInteger(Object value) {
        if (value == null) return 0;
        if (value instanceof Integer i) return i;
        if (value instanceof Number n) return n.intValue();
        return 0;
    }

    private Double toDouble(Object value) {
        if (value == null) return 0.0;
        if (value instanceof Double d) return d;
        if (value instanceof Number n) return n.doubleValue();
        return 0.0;
    }
}
