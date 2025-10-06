package com.affiliate.rentals.gydi.users.application.dto;

/**
 * DTO for authentication response containing JWT tokens.
 *
 * <p>This record is returned after successful login or token refresh,
 * containing the access token, refresh token, and user information.</p>
 *
 * @param token the JWT access token
 * @param refreshToken the refresh token (nullable)
 * @param tokenType the type of token (typically "Bearer")
 * @param user the authenticated user's information
 * @author GYDI Development Team
 */
public record AuthResponse(
        String token,
        String refreshToken,
        String tokenType,
        UserResponse user
) {
    /**
     * Creates an AuthResponse with "Bearer" as the default token type.
     *
     * @param token the JWT access token
     * @param refreshToken the refresh token
     * @param user the user information
     * @return a new AuthResponse instance
     */
    public static AuthResponse of(String token, String refreshToken, UserResponse user) {
        return new AuthResponse(token, refreshToken, "Bearer", user);
    }

    /**
     * Creates an AuthResponse for token refresh (no refresh token included).
     *
     * @param token the new JWT access token
     * @param user the user information
     * @return a new AuthResponse instance
     */
    public static AuthResponse ofRefresh(String token, UserResponse user) {
        return new AuthResponse(token, null, "Bearer", user);
    }
}
