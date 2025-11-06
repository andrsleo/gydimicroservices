package com.affiliate.rentals.gydi.users.application.dto;

import java.time.LocalDateTime;

/**
 * Response DTO for password reset token validation.
 *
 * <p>This record represents the result of validating a password reset token.
 * It follows the Data Transfer Object (DTO) pattern and uses Java records
 * for immutability and conciseness.</p>
 *
 * <p>Response fields:</p>
 * <ul>
 *   <li>valid: Whether the token is valid and can be used</li>
 *   <li>email: The email associated with the token (null if invalid)</li>
 *   <li>expiresAt: When the token expires (null if invalid)</li>
 *   <li>message: User-friendly message explaining the validation result</li>
 * </ul>
 *
 * <p>Example JSON response (valid token):</p>
 * <pre>{@code
 * {
 *   "valid": true,
 *   "email": "user@example.com",
 *   "expiresAt": "2025-10-28T15:30:00",
 *   "message": "Token is valid"
 * }
 * }</pre>
 *
 * <p>Example JSON response (invalid token):</p>
 * <pre>{@code
 * {
 *   "valid": false,
 *   "email": null,
 *   "expiresAt": null,
 *   "message": "Token has expired or is invalid"
 * }
 * }</pre>
 *
 * @author GYDI Development Team
 */
public record ValidateTokenResponse(
        boolean valid,
        String email,
        LocalDateTime expiresAt,
        String message
) {
    /**
     * Creates a valid token response.
     *
     * @param email the email associated with the token
     * @param expiresAt when the token expires
     * @return a ValidateTokenResponse indicating the token is valid
     */
    public static ValidateTokenResponse valid(String email, LocalDateTime expiresAt) {
        return new ValidateTokenResponse(true, email, expiresAt, "Token is valid");
    }

    /**
     * Creates an invalid token response with a custom message.
     *
     * @param message the reason why the token is invalid
     * @return a ValidateTokenResponse indicating the token is invalid
     */
    public static ValidateTokenResponse invalid(String message) {
        return new ValidateTokenResponse(false, null, null, message);
    }

    /**
     * Creates an invalid token response for expired tokens.
     *
     * @return a ValidateTokenResponse indicating the token has expired
     */
    public static ValidateTokenResponse expired() {
        return invalid("Password reset token has expired. Please request a new one.");
    }

    /**
     * Creates an invalid token response for already used tokens.
     *
     * @return a ValidateTokenResponse indicating the token was already used
     */
    public static ValidateTokenResponse alreadyUsed() {
        return invalid("Password reset token has already been used. Please request a new one.");
    }

    /**
     * Creates an invalid token response for non-existent tokens.
     *
     * @return a ValidateTokenResponse indicating the token doesn't exist
     */
    public static ValidateTokenResponse notFound() {
        return invalid("Invalid password reset token.");
    }
}
