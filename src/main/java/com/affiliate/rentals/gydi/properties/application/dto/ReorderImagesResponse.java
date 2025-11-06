package com.affiliate.rentals.gydi.properties.application.dto;

import java.util.UUID;

/**
 * Response DTO for reorder images operation.
 */
public record ReorderImagesResponse(
    boolean success,
    int updatedCount,
    UUID coverImageId,
    String message
) {
    public static ReorderImagesResponse success(int count, UUID coverImageId) {
        return new ReorderImagesResponse(
            true,
            count,
            coverImageId,
            "Images reordered successfully"
        );
    }
}