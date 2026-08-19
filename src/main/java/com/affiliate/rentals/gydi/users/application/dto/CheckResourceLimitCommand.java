package com.affiliate.rentals.gydi.users.application.dto;

import com.affiliate.rentals.gydi.users.domain.model.ResourceType;

import jakarta.validation.constraints.NotNull;

/**
 * Command for checking if a user can create more resources of a specific type.
 *
 * <p>This record encapsulates the data required to evaluate if a user has
 * reached their subscription plan limit for a specific resource type
 * (properties, referrals). It is used by the resource limit evaluation use case.</p>
 *
 * @param userId the unique identifier of the user
 * @param resourceType the type of resource to check
 * @author GYDI Development Team
 */
public record CheckResourceLimitCommand(
        @NotNull(message = "User ID is required")
        Long userId,

        @NotNull(message = "Resource type is required")
        ResourceType resourceType
) {
}