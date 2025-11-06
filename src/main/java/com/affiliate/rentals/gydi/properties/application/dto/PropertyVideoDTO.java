package com.affiliate.rentals.gydi.properties.application.dto;

import java.time.LocalDateTime;

/**
 * DTO for property video.
 */
public record PropertyVideoDTO(
    String id,
    String url,
    String thumbnailUrl,
    int displayOrder,
    Integer durationSeconds,
    LocalDateTime uploadedAt
) {}
