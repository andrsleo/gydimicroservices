package com.affiliate.rentals.gydi.collaborations.infrastructure.in.rest.dto;

import jakarta.validation.constraints.Size;

public record DeclinePitchRequest(
        @Size(max = 500) String reason
) {}
