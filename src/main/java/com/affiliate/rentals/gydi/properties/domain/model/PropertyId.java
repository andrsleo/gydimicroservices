package com.affiliate.rentals.gydi.properties.domain.model;

import java.util.Objects;
import java.util.UUID;

/**
 * Value object representing a property unique identifier
 */
public class PropertyId {
    private final UUID value;

    private PropertyId(UUID value) {
        Objects.requireNonNull(value, "PropertyId cannot be null");
        this.value = value;
    }

    public static PropertyId of(UUID value) {
        return new PropertyId(value);
    }

    public static PropertyId of(String value) {
        return new PropertyId(UUID.fromString(value));
    }

    public static PropertyId generate() {
        return new PropertyId(UUID.randomUUID());
    }

    public UUID getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PropertyId that = (PropertyId) o;
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
