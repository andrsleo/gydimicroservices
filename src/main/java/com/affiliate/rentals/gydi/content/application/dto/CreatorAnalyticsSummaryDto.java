package com.affiliate.rentals.gydi.content.application.dto;

import java.util.List;

/**
 * Creator analytics summary for the Admin dashboard.
 *
 * <ul>
 *   <li>{@code topCreators} — top 20 creators ordered by total views.</li>
 *   <li>{@code newCreatorsThisMonth} — count of creators who published their first post this month.</li>
 *   <li>{@code retentionRate} — ratio of creators active in the last 30 days vs. total creators (0.0–1.0).</li>
 * </ul>
 */
public record CreatorAnalyticsSummaryDto(
        List<CreatorLeaderboardDto> topCreators,
        Integer newCreatorsThisMonth,
        Double retentionRate
) {}
