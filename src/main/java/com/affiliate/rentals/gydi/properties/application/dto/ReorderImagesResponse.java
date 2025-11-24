package com.affiliate.rentals.gydi.properties.application.dto;

/**
 * Response DTO for reorder images operation.
 */
public record ReorderImagesResponse(
        boolean success,
        int updatedCount,
        Long coverImageId,
        String message) {
    public static ReorderImagesResponse success(int count, Long coverImageId) {
        return new ReorderImagesResponse(
                true,
                count,
                coverImageId,
                "Images reordered successfully");
    }
}