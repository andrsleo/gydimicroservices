package com.affiliate.rentals.gydi.properties.application.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO for property full details (single property view).
 */
public record PropertyDetailResponse(
        String id,
        Long hostId,
        String title,
        String slug,
        String description,
        BigDecimal pricePerNight,
        String currency,
        BigDecimal salePrice,
        PropertyLocationDTO location,
        List<String> amenities,
        PropertySpecsDTO specs,
        String propertyType,
        String listingType,
        String status,
        List<PropertyImageDTO> images,
        List<PropertyVideoDTO> videos,
        String coverImageId,
        int imageCount,
        int videoCount,
        // Airbnb import fields
        String airbnbUrl,
        String importMode,
        LocalDateTime importedAt,
        String airbnbListingId,
        String icalUrlAirbnb,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime publishedAt) {
}
