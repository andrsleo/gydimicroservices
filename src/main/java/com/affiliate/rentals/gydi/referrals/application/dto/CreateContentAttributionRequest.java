package com.affiliate.rentals.gydi.referrals.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreateContentAttributionRequest(
        Long bookingId,

        @NotNull(message = "contentPostId is required")
        @Positive(message = "contentPostId must be positive")
        Long contentPostId,

        @NotNull(message = "creatorId is required")
        @Positive(message = "creatorId must be positive")
        Long creatorId,

        Long referralLinkId,

        @NotBlank(message = "attributionType is required")
        String attributionType
) {}
