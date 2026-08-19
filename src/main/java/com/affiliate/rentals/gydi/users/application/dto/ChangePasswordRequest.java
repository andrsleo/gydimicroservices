package com.affiliate.rentals.gydi.users.application.dto;

import com.affiliate.rentals.gydi.shared.validation.StrongPassword;
import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for changing a user's password when authenticated.
 *
 * <p>
 * This record represents the data required for an authenticated user to change
 * their password. It follows the Data Transfer Object (DTO) pattern and uses
 * Java records for immutability and conciseness.
 * </p>
 *
 * <p>
 * Validation rules:
 * </p>
 * <ul>
 * <li>currentPassword: Must not be blank (verified against stored hash)</li>
 * <li>newPassword: Must meet strong password requirements (see {@link StrongPassword})</li>
 * </ul>
 *
 * <p>
 * Example JSON request:
 * </p>
 *
 * <pre>{@code
 * {
 *   "currentPassword": "OldP@ssw0rd",
 *   "newPassword": "NewSecureP@ssw0rd123"
 * }
 * }</pre>
 *
 * @author GYDI Development Team
 * @see StrongPassword
 */
public record ChangePasswordRequest(
        @NotBlank(message = "Current password is required")
        String currentPassword,

        @NotBlank(message = "New password is required")
        @StrongPassword
        String newPassword) {
}
