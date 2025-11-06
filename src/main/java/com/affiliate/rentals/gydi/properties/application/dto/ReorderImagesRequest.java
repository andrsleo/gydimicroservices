package com.affiliate.rentals.gydi.properties.application.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

/**
 * Request DTO for reordering property images.
 * Includes list of image orders and optional cover image selection.
 */
public record ReorderImagesRequest(
    @NotNull(message = "Image orders cannot be null")
    @NotEmpty(message = "At least one image order is required")
    @Valid
    List<ImageOrderItem> imageOrders,

    UUID coverImageId
) {
    /**
     * Single image order item containing image ID and new display order.
     */
    public record ImageOrderItem(
        @NotNull(message = "Image ID cannot be null")
        UUID imageId,

        @NotNull(message = "Display order cannot be null")
        @Min(value = 0, message = "Display order must be >= 0")
        @Max(value = 19, message = "Display order must be <= 19")
        Integer displayOrder
    ) {}
}