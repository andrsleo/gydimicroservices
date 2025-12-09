package com.affiliate.rentals.gydi.properties.application.dto;

/**
 * Response DTO for Airbnb URL validation result.
 */
public record ValidateAirbnbUrlResponse(
        boolean valid,
        String listingId,
        String title,
        String description,
        String imageUrl,
        String errorMessage) {
}
