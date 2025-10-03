package com.affiliate.rentals.gydi.properties.domain.model;

import java.util.Objects;

/**
 * Location value object - represents a property location.
 * Immutable by design following DDD principles.
 */
public record Location(String address) {

    public Location {
        Objects.requireNonNull(address, "Address cannot be null");
        if (address.isBlank()) {
            throw new IllegalArgumentException("Address cannot be blank");
        }
    }

    public static Location of(String address) {
        return new Location(address);
    }

    @Override
    public String toString() {
        return address;
    }
}
