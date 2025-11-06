package com.affiliate.rentals.gydi.users.application.dto;

import java.util.Set;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO for creating a new user.
 *
 * <p>This record encapsulates the data required to create a new user account.
 * It includes validation annotations to ensure data integrity at the API boundary.</p>
 *
 * <p><b>IMPORTANT:</b> The {@code name} parameter is deprecated. Use {@code firstName}
 * and {@code lastName} instead. Names are now stored in {@code user_profile} table
 * as the source of truth.</p>
 *
 * @param email the user's email address (must be valid email format)
 * @param password the user's password (minimum 8 characters)
 * @param firstName the user's first name (stored in user_profile)
 * @param lastName the user's last name (stored in user_profile)
 * @param name the user's full name (DEPRECATED - kept for backwards compatibility only)
 * @param phoneNumber the user's phone number (optional)
 * @param roleNames the set of role names to assign to the user
 * @author GYDI Development Team
 */
public record CreateUserRequest(
        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        String email,

        @NotBlank(message = "Password is required")
        @Size(min = 8, message = "Password must be at least 8 characters")
        String password,

        @NotBlank(message = "First name is required")
        @Size(max = 100, message = "First name must not exceed 100 characters")
        String firstName,

        @Size(max = 100, message = "Last name must not exceed 100 characters")
        String lastName,

        // DEPRECATED: Use firstName and lastName instead
        String name,

        String phoneNumber,

        Set<String> roleNames
) {
}
