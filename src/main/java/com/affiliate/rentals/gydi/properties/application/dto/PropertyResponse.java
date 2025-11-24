package com.affiliate.rentals.gydi.properties.application.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;


/**
 * DTO for property summary (used in lists).
 */
public record PropertyResponse(
    String id,
    Long hostId,
    String title,
    String slug,
    String description,
    BigDecimal pricePerNight,
    String currency,
    BigDecimal salePrice,
    String country,
    String city,
    int bedrooms,
    int bathrooms,
    int maxGuests,
    String propertyType,
    String listingType,
    String status,
    int imageCount,
    int videoCount,
    String mainImageUrl,
    LocalDateTime createdAt,
    LocalDateTime publishedAt
) {}
