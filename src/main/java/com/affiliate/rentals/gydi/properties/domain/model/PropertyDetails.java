package com.affiliate.rentals.gydi.properties.domain.model;

import java.util.Objects;

/**
 * PropertyDetails value object - encapsulates property physical characteristics.
 * Immutable by design following DDD principles.
 */
public record PropertyDetails(
        Integer bedrooms,
        Integer bathrooms,
        Integer beds,
        Integer capacity
) {

    public PropertyDetails {
        Objects.requireNonNull(capacity, "Capacity cannot be null");
        if (capacity <= 0) {
            throw new IllegalArgumentException("Capacity must be greater than 0");
        }
        if (bedrooms != null && bedrooms < 0) {
            throw new IllegalArgumentException("Bedrooms cannot be negative");
        }
        if (bathrooms != null && bathrooms < 0) {
            throw new IllegalArgumentException("Bathrooms cannot be negative");
        }
        if (beds != null && beds < 0) {
            throw new IllegalArgumentException("Beds cannot be negative");
        }
    }

    public static PropertyDetails of(Integer bedrooms, Integer bathrooms, Integer beds, Integer capacity) {
        return new PropertyDetails(bedrooms, bathrooms, beds, capacity);
    }

    public static PropertyDetails withCapacity(Integer capacity) {
        return new PropertyDetails(null, null, null, capacity);
    }

    public boolean canAccommodate(int numberOfGuests) {
        return capacity >= numberOfGuests;
    }

    @Override
    public String toString() {
        return "PropertyDetails[bedrooms=%d, bathrooms=%d, beds=%d, capacity=%d]"
                .formatted(bedrooms, bathrooms, beds, capacity);
    }
}
