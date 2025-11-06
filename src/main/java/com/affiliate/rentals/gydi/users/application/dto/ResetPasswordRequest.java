package com.affiliate.rentals.gydi.users.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for resetting a user's password using a reset token.
 *
 * <p>This record represents the data required to complete a password reset.
 * It follows the Data Transfer Object (DTO) pattern and uses Java records
 * for immutability and conciseness.</p>
 *
 * <p>Validation rules:</p>
 * <ul>
 *   <li>token: Must not be blank (UUID format)</li>
 *   <li>newPassword: Must be between 8-100 characters</li>
 *   <li>newPassword: Must contain at least one uppercase, one lowercase, one digit, and one special character</li>
 * </ul>
 *
 * <p>Example JSON request:</p>
 * <pre>{@code
 * {
 *   "token": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
 *   "newPassword": "NewSecureP@ssw0rd"
 * }
 * }</pre>
 *
 * @author GYDI Development Team
 */
public record ResetPasswordRequest(
        @NotBlank(message = "Reset token is required")
        String token,

        @NotBlank(message = "New password is required")
        @Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters")
        @Pattern(
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]+$",
                message = "Password must contain at least one uppercase letter, one lowercase letter, one number, and one special character (@$!%*?&)"
        )
        String newPassword
) {
}
