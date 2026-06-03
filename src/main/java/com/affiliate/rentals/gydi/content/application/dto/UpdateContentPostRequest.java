package com.affiliate.rentals.gydi.content.application.dto;

import jakarta.validation.constraints.Size;

/**
 * Request DTO for updating an existing content post.
 *
 * @param caption    updated caption text (max 500 chars)
 * @param propertyId updated linked property ID (may be null to unlink)
 * @author GYDI Development Team
 */
public record UpdateContentPostRequest(
        @Size(max = 500, message = "El caption no puede superar 500 caracteres")
        String caption,

        Long propertyId
) {
}
