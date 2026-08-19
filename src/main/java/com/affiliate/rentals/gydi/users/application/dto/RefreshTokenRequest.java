package com.affiliate.rentals.gydi.users.application.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * DTO for refresh token requests.
 *
 * <p>This record encapsulates the refresh token required to obtain a new access token.</p>
 *
 * @param refreshToken the refresh token string
 * @author GYDI Development Team
 */
public record RefreshTokenRequest(
        @NotBlank(message = "Refresh token is required")
        String refreshToken
) {
}