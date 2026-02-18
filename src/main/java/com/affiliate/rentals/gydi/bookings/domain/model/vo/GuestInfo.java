package com.affiliate.rentals.gydi.bookings.domain.model.vo;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Value Object representing guest information.
 * <p>
 * Immutable and validates data consistency.
 * </p>
 */
public final class GuestInfo {
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );

    private final String name;
    private final String email;
    private final String phone;
    private final int guestsCount;

    private GuestInfo(String name, String email, String phone, int guestsCount) {
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.guestsCount = guestsCount;
    }

    /**
     * Creates GuestInfo with validation.
     *
     * @param name guest full name (required)
     * @param email guest email (required, must be valid)
     * @param phone guest phone (optional)
     * @param guestsCount number of guests (must be 1-50)
     * @return validated GuestInfo
     * @throws IllegalArgumentException if validation fails
     */
    public static GuestInfo of(String name, String email, String phone, int guestsCount) {
        validateName(name);
        validateEmail(email);
        validateGuestsCount(guestsCount);

        return new GuestInfo(name.trim(), email.trim().toLowerCase(), phone != null ? phone.trim() : null, guestsCount);
    }

    private static void validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Guest name is required");
        }
        if (name.length() > 255) {
            throw new IllegalArgumentException("Guest name cannot exceed 255 characters");
        }
    }

    private static void validateEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Guest email is required");
        }
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            throw new IllegalArgumentException("Invalid email format: " + email);
        }
        if (email.length() > 255) {
            throw new IllegalArgumentException("Email cannot exceed 255 characters");
        }
    }

    private static void validateGuestsCount(int count) {
        if (count < 1 || count > 50) {
            throw new IllegalArgumentException("Guests count must be between 1 and 50, got: " + count);
        }
    }

    // Getters
    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public int getGuestsCount() {
        return guestsCount;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GuestInfo guestInfo = (GuestInfo) o;
        return guestsCount == guestInfo.guestsCount &&
               Objects.equals(name, guestInfo.name) &&
               Objects.equals(email, guestInfo.email) &&
               Objects.equals(phone, guestInfo.phone);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, email, phone, guestsCount);
    }

    @Override
    public String toString() {
        return String.format("GuestInfo{name='%s', email='%s', guests=%d}", name, email, guestsCount);
    }
}
