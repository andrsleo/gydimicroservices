package com.affiliate.rentals.gydi.users.domain.model;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Email value object representing a validated email address.
 *
 * <p>This is an immutable value object following Domain-Driven Design (DDD) principles.
 * It ensures email validity through pattern matching and provides a type-safe way to
 * handle email addresses throughout the application. The validation pattern supports
 * standard email formats including plus addressing and subdomain structures.</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * Email email = Email.of("user@example.com");
 * String address = email.address();
 * }</pre>
 *
 * @param address the validated email address string
 * @author GYDI Development Team
 */
public record Email(String address) {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );

    /**
     * Compact constructor that validates the email address.
     *
     * @throws NullPointerException if address is {@code null}
     * @throws IllegalArgumentException if address is blank or doesn't match email format
     */
    public Email {
        Objects.requireNonNull(address, "Email address cannot be null");
        if (address.isBlank()) {
            throw new IllegalArgumentException("Email address cannot be blank");
        }
        if (!EMAIL_PATTERN.matcher(address).matches()) {
            throw new IllegalArgumentException("Invalid email format: " + address);
        }
    }

    /**
     * Factory method to create an Email instance from a string address.
     *
     * @param address the email address string to validate
     * @return a validated Email instance
     * @throws NullPointerException if address is {@code null}
     * @throws IllegalArgumentException if address is blank or invalid
     */
    public static Email of(String address) {
        return new Email(address);
    }

    /**
     * Returns the string representation of this email address.
     *
     * @return the email address as a string
     */
    @Override
    public String toString() {
        return address;
    }
}
