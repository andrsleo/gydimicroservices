package com.affiliate.rentals.gydi.users.application.dto;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * DTO for user response data.
 *
 * <p>This record represents user data returned to clients. It excludes sensitive
 * information like passwords and includes only publicly accessible user information.</p>
 *
 * @param id the user's unique identifier
 * @param email the user's email address
 * @param name the user's name
 * @param phoneNumber the user's phone number
 * @param roleNames the set of roles assigned to the user
 * @param createdAt the timestamp when the user was created
 * @author GYDI Development Team
 */
public record UserResponse(
        Long id,
        String email,
        String name,
        String phoneNumber,
        Set<String> roleNames,
        LocalDateTime createdAt
) {
}
