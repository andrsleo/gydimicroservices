package com.affiliate.rentals.gydi.users.application.dto;

/**
 * DTO for authentication response containing JWT token.
 *
 * <p>This record is returned after successful login, containing the JWT token
 * and user information.</p>
 *
 * @param token the JWT access token
 * @param tokenType the type of token (typically "Bearer")
 * @param user the authenticated user's information
 * @author GYDI Development Team
 */
public record AuthResponse(
        String token,
        String tokenType,
        UserResponse user
) {
    /**
     * Creates an AuthResponse with "Bearer" as the default token type.
     *
     * @param token the JWT token
     * @param user the user information
     * @return a new AuthResponse instance
     */
    public static AuthResponse of(String token, UserResponse user) {
        return new AuthResponse(token, "Bearer", user);
    }
}
