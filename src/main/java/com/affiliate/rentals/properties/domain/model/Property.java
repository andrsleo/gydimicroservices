package com.affiliate.rentals.properties.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import lombok.Builder;

/**
 * Domain model: pure business representation of a Property.
 * No framework or persistence annotations here.
 */
@Builder
public record Property(
        UUID id,
        UUID ownerId,
        String title,
        String description,
        String location,
        BigDecimal pricePerNight,
        String currency,
        Integer bedrooms,
        Integer bathrooms,
        Integer beds,
        Integer capacity,
        Instant createdAt
) {

    // Ejemplo de validación de negocio
    public Property {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Property title must not be blank");
        }
        if (pricePerNight == null || pricePerNight.signum() <= 0) {
            throw new IllegalArgumentException("Price per night must be greater than 0");
        }
        if (capacity == null || capacity <= 0) {
            throw new IllegalArgumentException("Capacity must be greater than 0");
        }
        if (currency == null || currency.isBlank()) {
            throw new IllegalArgumentException("Currency must not be blank");
        }
    }
}
