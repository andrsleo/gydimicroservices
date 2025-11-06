package com.affiliate.rentals.gydi.users.application.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for initiating a password reset process.
 *
 * <p>This record represents the data required to request a password reset.
 * It follows the Data Transfer Object (DTO) pattern and uses Java records
 * for immutability and conciseness.</p>
 *
 * <p>Validation rules:</p>
 * <ul>
 *   <li>email: Must be a valid email format and not blank</li>
 * </ul>
 *
 * <p>Example JSON request:</p>
 * <pre>{@code
 * {
 *   "email": "user@example.com"
 * }
 * }</pre>
 *
 * <p>Security note: The response should always return success regardless of whether
 * the email exists in the system, to prevent email enumeration attacks.</p>
 *
 * @author GYDI Development Team
 */
public record ForgotPasswordRequest(
        @NotBlank(message = "Email is required")
        @Email(message = "Email must be a valid email address")
        String email
) {
}
