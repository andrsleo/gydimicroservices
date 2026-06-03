package com.affiliate.rentals.gydi.content.application.dto;

import com.affiliate.rentals.gydi.content.domain.model.ContentType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for creating a new content post.
 *
 * @param caption    the sanitized caption text (max 500 chars)
 * @param type       the type of content (VIDEO, PHOTO, CAROUSEL)
 * @param propertyId optional linked property ID
 * @author GYDI Development Team
 */
public record CreateContentPostRequest(
        @Size(max = 500, message = "El caption no puede superar 500 caracteres")
        String caption,

        @NotNull(message = "El tipo de contenido es requerido")
        ContentType type,

        @NotNull(message = "La propiedad es requerida")
        Long propertyId
) {
}
