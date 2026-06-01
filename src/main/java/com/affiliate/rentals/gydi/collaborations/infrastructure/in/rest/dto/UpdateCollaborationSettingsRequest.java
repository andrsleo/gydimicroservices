package com.affiliate.rentals.gydi.collaborations.infrastructure.in.rest.dto;

import jakarta.validation.constraints.NotNull;

import java.util.List;

public record UpdateCollaborationSettingsRequest(
        @NotNull Boolean acceptCollaborations,
        @NotNull List<String> acceptedCompensations
) {}
