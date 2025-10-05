package com.affiliate.rentals.gydi.users.application.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * DTO for user login requests.
 *
 * <p>This record encapsulates the credentials required for user authentication.</p>
 *
 * @param email the user's email address
 * @param password the user's password
 * @author GYDI Development Team
 */
public record LoginRequest(
        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        String email,

        @NotBlank(message = "Password is required")
        String password
) {
}
