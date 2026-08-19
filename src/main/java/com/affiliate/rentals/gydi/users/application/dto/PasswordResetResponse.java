package com.affiliate.rentals.gydi.users.application.dto;

/**
 * Generic response DTO for password reset operations.
 *
 * <p>This record provides a standard response format for password reset endpoints.
 * It follows the Data Transfer Object (DTO) pattern and uses Java records
 * for immutability and conciseness.</p>
 *
 * <p>Response fields:</p>
 * <ul>
 *   <li>success: Whether the operation was successful</li>
 *   <li>message: User-friendly message about the operation result</li>
 * </ul>
 *
 * <p>Example JSON response:</p>
 * <pre>{@code
 * {
 *   "success": true,
 *   "message": "If an account exists with that email, a password reset link has been sent."
 * }
 * }</pre>
 *
 * @author GYDI Development Team
 */
public record PasswordResetResponse(
        boolean success,
        String message
) {
    /**
     * Creates a success response for password reset request.
     * Note: This message is intentionally generic to prevent email enumeration.
     *
     * @return a PasswordResetResponse indicating success
     */
    public static PasswordResetResponse resetRequested() {
        return new PasswordResetResponse(
                true,
                "If an account exists with that email, a password reset link has been sent."
        );
    }

    /**
     * Creates a success response for password reset completion.
     *
     * @return a PasswordResetResponse indicating password was reset successfully
     */
    public static PasswordResetResponse passwordResetSuccess() {
        return new PasswordResetResponse(
                true,
                "Your password has been successfully reset. You can now log in with your new password."
        );
    }

    /**
     * Creates an error response for rate limiting.
     *
     * @return a PasswordResetResponse indicating too many requests
     */
    public static PasswordResetResponse rateLimitExceeded() {
        return new PasswordResetResponse(
                false,
                "Too many password reset requests. Please try again later."
        );
    }

    /**
     * Creates an error response for invalid or expired token.
     *
     * @return a PasswordResetResponse indicating invalid token
     */
    public static PasswordResetResponse invalidToken() {
        return new PasswordResetResponse(
                false,
                "Invalid or expired password reset token. Please request a new one."
        );
    }

    /**
     * Creates a custom error response.
     *
     * @param message the error message
     * @return a PasswordResetResponse with the provided error message
     */
    public static PasswordResetResponse error(String message) {
        return new PasswordResetResponse(false, message);
    }
}
