package com.affiliate.rentals.gydi.properties.domain.model;

import java.util.Objects;

/**
 * PropertySpecs value object representing property specifications.
 */
public record PropertySpecs(
        int bedrooms,
        int bathrooms,
        int maxGuests
) {
    public PropertySpecs {
        if (bedrooms < 0) {
            throw new IllegalArgumentException("Bedrooms cannot be negative");
        }
        if (bathrooms < 0) {
            throw new IllegalArgumentException("Bathrooms cannot be negative");
        }
        if (maxGuests <= 0) {
            throw new IllegalArgumentException("Max guests must be positive");
        }
    }

    public static PropertySpecs of(int bedrooms, int bathrooms, int maxGuests) {
        return new PropertySpecs(bedrooms, bathrooms, maxGuests);
    }

    public boolean canAccommodate(int numberOfGuests) {
        return maxGuests >= numberOfGuests;
    }

    @Override
    public String toString() {
        return "%d bedrooms, %d bathrooms, max %d guests"
                .formatted(bedrooms, bathrooms, maxGuests);
    }
}
