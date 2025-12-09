package com.affiliate.rentals.gydi.properties.application.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for validating an Airbnb URL.
 */
public record ValidateAirbnbUrlRequest(
    @NotBlank(message = "Airbnb URL is required")
    String airbnbUrl
) {}
