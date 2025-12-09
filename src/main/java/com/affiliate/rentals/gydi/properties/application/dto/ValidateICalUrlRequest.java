package com.affiliate.rentals.gydi.properties.application.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for validating iCal URLs.
 */
public record ValidateICalUrlRequest(
        @NotBlank(message = "iCal URL is required") String icalUrl) {
}
