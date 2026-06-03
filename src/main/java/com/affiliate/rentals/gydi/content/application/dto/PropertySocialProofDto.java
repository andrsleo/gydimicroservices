package com.affiliate.rentals.gydi.content.application.dto;

import java.util.List;

/**
 * Social proof aggregated stats for a property:
 * how much creator content it has and the social commerce performance.
 */
public record PropertySocialProofDto(
    Long propertyId,
    Integer contentCount,
    Long totalViews,
    Integer totalBookings,
    List<CreatorSummaryDto> topCreators
) {}
