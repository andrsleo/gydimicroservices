package com.affiliate.rentals.gydi.properties.application.dto;

/**
 * Response DTO for iCal URL validation.
 */
public record ValidateICalUrlResponse(
        boolean valid,
        String message) {
    public static ValidateICalUrlResponse of(boolean valid, String message) {
        return new ValidateICalUrlResponse(valid, message);
    }
}
