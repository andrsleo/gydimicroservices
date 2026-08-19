package com.affiliate.rentals.gydi.properties.application.dto;

import java.time.LocalDateTime;

/**
 * DTO for property image.
 */
public record PropertyImageDTO(
    String id,
    String url,
    int displayOrder,
    LocalDateTime uploadedAt
) {}
