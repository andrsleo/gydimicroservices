package com.affiliate.rentals.gydi.users.application.dto;

import java.time.LocalDateTime;

public record CreatorContentAnalyticsDto(
        Long contentPostId,
        String caption,
        String thumbnailUrl,
        Long views,
        Integer likes,
        Integer saves,
        Integer bookingsGenerated,
        Double earnings,
        LocalDateTime publishedAt
) {}
