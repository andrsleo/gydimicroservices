package com.affiliate.rentals.gydi.users.application.dto;

import java.time.LocalDateTime;

public record CreatorEarningsDto(
        Long contentPostId,
        String contentCaption,
        Long bookingId,
        String attributionType,
        Double amount,
        LocalDateTime earnedAt
) {}
