package com.affiliate.rentals.gydi.referrals.application.dto;

import java.time.LocalDateTime;

public record ContentAttributionDto(
        Long id,
        Long bookingId,
        Long contentPostId,
        Long creatorId,
        Long referralLinkId,
        String attributionType,
        LocalDateTime createdAt
) {}
