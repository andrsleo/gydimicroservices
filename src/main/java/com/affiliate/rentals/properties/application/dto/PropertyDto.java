package com.affiliate.rentals.properties.application.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PropertyDto(
    UUID id,
    UUID ownerId,
    String title,
    String description,
    String location,
    BigDecimal pricePerNight,
    String currency,
    int bedrooms,
    int bathrooms,
    int beds,
    int capacity,
    Instant createdAt
) {}
