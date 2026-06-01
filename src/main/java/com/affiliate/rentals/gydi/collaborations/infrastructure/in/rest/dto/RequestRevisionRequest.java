package com.affiliate.rentals.gydi.collaborations.infrastructure.in.rest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RequestRevisionRequest(
        @NotBlank @Size(max = 500) String feedback
) {}
