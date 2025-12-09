package com.affiliate.rentals.gydi.users.application.dto;

import com.affiliate.rentals.gydi.shared.validation.StrongPassword;
import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for resetting a user's password using a reset token.
 *
 * <p>
 * This record represents the data required to complete a password reset.
 * It follows the Data Transfer Object (DTO) pattern and uses Java records
 * for immutability and conciseness.
 * </p>
 *
 * <p>
 * Validation rules:
 * </p>
 * <ul>
 * <li>token: Must not be blank</li>
 * <li>newPassword: Must meet strong password requirements (see {@link StrongPassword})</li>
 * </ul>
 *
 * <p>
 * Example JSON request:
 * </p>
 *
 * <pre>{@code
 * {
 *   "token": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
 *   "newPassword": "NewSecureP@ssw0rd"
 * }
 * }</pre>
 *
 * @author GYDI Development Team
 * @see StrongPassword
 */
public record ResetPasswordRequest(
                @NotBlank(message = "Reset token is required")
                String token,

                @NotBlank(message = "New password is required")
                @StrongPassword
                String newPassword) {
}
