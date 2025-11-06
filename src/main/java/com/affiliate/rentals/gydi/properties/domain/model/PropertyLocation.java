package com.affiliate.rentals.gydi.properties.domain.model;

import java.util.Objects;

/**
 * PropertyLocation value object representing detailed location information.
 */
public record PropertyLocation(
        String country,
        String city,
        String address,
        String postalCode
) {
    public PropertyLocation {
        Objects.requireNonNull(country, "Country cannot be null");
        Objects.requireNonNull(city, "City cannot be null");

        if (country.isBlank()) {
            throw new IllegalArgumentException("Country cannot be blank");
        }
        if (city.isBlank()) {
            throw new IllegalArgumentException("City cannot be blank");
        }
    }

    public static PropertyLocation of(String country, String city, String address, String postalCode) {
        return new PropertyLocation(country, city, address, postalCode);
    }

    public static PropertyLocation of(String country, String city) {
        return new PropertyLocation(country, city, null, null);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (address != null && !address.isBlank()) {
            sb.append(address).append(", ");
        }
        sb.append(city).append(", ").append(country);
        if (postalCode != null && !postalCode.isBlank()) {
            sb.append(" ").append(postalCode);
        }
        return sb.toString();
    }
}
