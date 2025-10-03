package com.affiliate.rentals.gydi.properties.domain.model;

import java.util.Objects;

/**
 * Amenity value object representing a property amenity or feature.
 *
 * <p>This is an immutable value object following Domain-Driven Design (DDD) principles.
 * It represents features and facilities available at a rental property such as WiFi,
 * parking, pool, air conditioning, etc. Each amenity has a unique identifier and a
 * descriptive name.</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * Amenity wifi = Amenity.withName("WiFi");
 * Amenity pool = Amenity.of(1L, "Swimming Pool");
 * }</pre>
 *
 * @param id the amenity's unique identifier, may be {@code null} for transient amenities
 * @param name the amenity name, never {@code null} or blank
 * @author GYDI Development Team
 */
public record Amenity(Long id, String name) {

    /**
     * Compact constructor that validates the amenity name.
     *
     * @throws NullPointerException if name is {@code null}
     * @throws IllegalArgumentException if name is blank
     */
    public Amenity {
        Objects.requireNonNull(name, "Amenity name cannot be null");
        if (name.isBlank()) {
            throw new IllegalArgumentException("Amenity name cannot be blank");
        }
    }

    /**
     * Factory method to create an Amenity with a specific ID and name.
     *
     * @param id the amenity ID
     * @param name the amenity name
     * @return a new Amenity instance
     * @throws NullPointerException if name is {@code null}
     * @throws IllegalArgumentException if name is blank
     */
    public static Amenity of(Long id, String name) {
        return new Amenity(id, name);
    }

    /**
     * Factory method to create an Amenity with only a name (no ID).
     * Useful for creating transient amenities before persistence.
     *
     * @param name the amenity name
     * @return a new Amenity instance with null ID
     * @throws NullPointerException if name is {@code null}
     * @throws IllegalArgumentException if name is blank
     */
    public static Amenity withName(String name) {
        return new Amenity(null, name);
    }
}
