package com.affiliate.rentals.gydi.users.application.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.Set;

/**
 * DTO for updating an existing user.
 *
 * <p>This record encapsulates the data that can be updated for an existing user.
 * Email cannot be changed after account creation for security reasons.</p>
 *
 * @param name the user's name
 * @param phoneNumber the user's phone number (optional)
 * @param roleNames the set of role names to assign to the user
 * @author GYDI Development Team
 */
public record UpdateUserRequest(
        @NotBlank(message = "Name is required")
        String name,

        String phoneNumber,

        Set<String> roleNames
) {
}
