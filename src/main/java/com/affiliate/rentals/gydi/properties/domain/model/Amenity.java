package com.affiliate.rentals.gydi.properties.domain.model;

import java.util.Objects;

/**
 * Amenity value object - represents a property amenity.
 * Immutable by design following DDD principles.
 */
public record Amenity(Long id, String name) {

    public Amenity {
        Objects.requireNonNull(name, "Amenity name cannot be null");
        if (name.isBlank()) {
            throw new IllegalArgumentException("Amenity name cannot be blank");
        }
    }

    public static Amenity of(Long id, String name) {
        return new Amenity(id, name);
    }

    public static Amenity withName(String name) {
        return new Amenity(null, name);
    }
}
