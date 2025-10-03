package com.affiliate.rentals.gydi.properties.domain.model;

import java.util.Objects;

/**
 * Location value object representing a property's geographic location.
 *
 * <p>This is an immutable value object following Domain-Driven Design (DDD) principles.
 * It encapsulates a property's address information and ensures validity through validation.
 * The address is stored as a simple string that can represent any format (street address,
 * city, coordinates, etc.).</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * Location location = Location.of("123 Main St, Springfield, IL 62701");
 * String address = location.address();
 * }</pre>
 *
 * @param address the property address, never {@code null} or blank
 * @author GYDI Development Team
 */
public record Location(String address) {

    /**
     * Compact constructor that validates the address.
     *
     * @throws NullPointerException if address is {@code null}
     * @throws IllegalArgumentException if address is blank
     */
    public Location {
        Objects.requireNonNull(address, "Address cannot be null");
        if (address.isBlank()) {
            throw new IllegalArgumentException("Address cannot be blank");
        }
    }

    /**
     * Factory method to create a Location from an address string.
     *
     * @param address the property address
     * @return a new Location instance
     * @throws NullPointerException if address is {@code null}
     * @throws IllegalArgumentException if address is blank
     */
    public static Location of(String address) {
        return new Location(address);
    }

    /**
     * Returns the string representation of this location.
     *
     * @return the address as a string
     */
    @Override
    public String toString() {
        return address;
    }
}
