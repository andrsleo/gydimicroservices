package com.affiliate.rentals.gydi.users.application.dto;

import com.affiliate.rentals.gydi.users.domain.model.Permission;

import jakarta.validation.constraints.NotNull;

/**
 * Command for checking if a user has a specific permission.
 *
 * <p>This record encapsulates the data required to evaluate if a user has
 * permission to perform a specific action. It is used by the permission
 * evaluation use case.</p>
 *
 * @param userId the unique identifier of the user
 * @param permission the permission to check
 * @author GYDI Development Team
 */
public record CheckPermissionCommand(
        @NotNull(message = "User ID is required")
        Long userId,

        @NotNull(message = "Permission is required")
        Permission permission
) {
}