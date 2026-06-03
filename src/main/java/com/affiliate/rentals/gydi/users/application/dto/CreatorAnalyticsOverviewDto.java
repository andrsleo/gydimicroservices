package com.affiliate.rentals.gydi.users.application.dto;

import com.affiliate.rentals.gydi.users.domain.model.CreatorTier;

public record CreatorAnalyticsOverviewDto(
        Long totalViews,
        Integer totalLikes,
        Integer totalSaves,
        Integer totalBookings,
        Double totalEarnings,
        Integer followerCount,
        Integer contentCount,
        Double avgEngagementRate,
        CreatorTier tier
) {}
