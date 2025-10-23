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
 * @param email the user's email address (must be valid email format)
 * @param password the user's password (minimum 8 characters)
 * @param name the user's name
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

        @NotBlank(message = "Name is required")
        String name,

        String phoneNumber,

        Set<String> roleNames
) {
}
