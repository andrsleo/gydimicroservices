package com.affiliate.rentals.gydi.content.application.dto;

/**
 * Attribution funnel metrics for the Admin dashboard.
 *
 * <ul>
 *   <li>{@code totalBookingsFromContent} — count of bookings linked to a content post via {@code referrals.content_attributions}.</li>
 *   <li>{@code conversionRate} — bookings from content / total content views (0.0–1.0).</li>
 *   <li>{@code avgAttributedRevenue} — average {@code total_price} of bookings attributed to content.</li>
 *   <li>{@code totalContentViews} — total view_count across all published posts.</li>
 *   <li>{@code totalContentWithProperty} — count of published posts linked to a property.</li>
 * </ul>
 */
public record AttributionFunnelDto(
        Integer totalBookingsFromContent,
        Double conversionRate,
        Double avgAttributedRevenue,
        Long totalContentViews,
        Long totalContentWithProperty
) {}
