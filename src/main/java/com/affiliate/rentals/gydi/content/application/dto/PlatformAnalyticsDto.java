package com.affiliate.rentals.gydi.content.application.dto;

/**
 * Platform-wide analytics summary for the Admin dashboard.
 *
 * <ul>
 *   <li>{@code gmv} — Gross Merchandise Value: sum of confirmed booking total_price (USD).</li>
 *   <li>{@code viralCoefficient} — rough estimate: total shares / total published posts.</li>
 *   <li>{@code contentVelocity} — daily average of posts published in the last 7 days.</li>
 *   <li>{@code totalCreators} — count of users who have at least one published post.</li>
 *   <li>{@code totalViews} — aggregate from {@code content.daily_content_stats}.</li>
 *   <li>{@code totalPosts} — total published posts across the platform.</li>
 * </ul>
 */
public record PlatformAnalyticsDto(
        Double gmv,
        Double viralCoefficient,
        Double contentVelocity,
        Integer totalCreators,
        Long totalViews,
        Long totalPosts
) {}
