package com.affiliate.rentals.gydi.properties.application.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record ReorderVideosRequest(
    @NotNull(message = "Video orders cannot be null")
    @NotEmpty(message = "At least one video order is required")
    @Valid List<VideoOrderItem> videoOrders) {

    public record VideoOrderItem(
        @NotNull(message = "Video ID cannot be null") Long videoId,
        @NotNull(message = "Display order cannot be null")
        @Min(value = 0, message = "Display order must be >= 0")
        @Max(value = 1, message = "Display order must be <= 1") Integer displayOrder) {
    }
}
