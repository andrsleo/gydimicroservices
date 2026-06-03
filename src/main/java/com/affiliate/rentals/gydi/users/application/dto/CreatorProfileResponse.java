package com.affiliate.rentals.gydi.users.application.dto;

import com.affiliate.rentals.gydi.users.domain.model.CreatorTier;

import java.time.LocalDateTime;

public record CreatorProfileResponse(
        Long userId,
        Integer contentCount,
        Integer followerCount,
        Integer followingCount,
        Long totalViews,
        Double avgEngagementRate,
        CreatorTier tier,
        LocalDateTime lastContentAt,
        LocalDateTime updatedAt
) {}
