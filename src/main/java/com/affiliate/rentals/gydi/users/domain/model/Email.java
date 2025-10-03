package com.affiliate.rentals.gydi.users.domain.model;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Email value object - ensures email validity.
 * Immutable and self-validating following DDD principles.
 */
public record Email(String address) {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );

    public Email {
        Objects.requireNonNull(address, "Email address cannot be null");
        if (address.isBlank()) {
            throw new IllegalArgumentException("Email address cannot be blank");
        }
        if (!EMAIL_PATTERN.matcher(address).matches()) {
            throw new IllegalArgumentException("Invalid email format: " + address);
        }
    }

    public static Email of(String address) {
        return new Email(address);
    }

    @Override
    public String toString() {
        return address;
    }
}
