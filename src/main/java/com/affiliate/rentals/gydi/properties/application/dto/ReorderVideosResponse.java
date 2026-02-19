package com.affiliate.rentals.gydi.properties.application.dto;

public record ReorderVideosResponse(
        boolean success,
        int updatedCount,
        String message) {
    public static ReorderVideosResponse success(int count) {
        return new ReorderVideosResponse(true, count, "Videos reordered successfully");
    }
}
