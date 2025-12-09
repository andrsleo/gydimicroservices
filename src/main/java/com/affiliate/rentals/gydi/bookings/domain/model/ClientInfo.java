package com.affiliate.rentals.gydi.bookings.domain.model;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Value Object representing client information for a booking.
 * Immutable and self-validating.
 * <p>
 * Contains PII (Personally Identifiable Information) - must be encrypted in database.
 */
public final class ClientInfo {
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );
    private static final Pattern PHONE_PATTERN = Pattern.compile(
        "^\\+?[1-9]\\d{1,14}$" // E.164 format (international phone numbers)
    );

    private final String email;
    private final String phone; // Optional
    private final String firstName;
    private final String lastName;

    /**
     * Creates client information.
     *
     * @param email the client's email (required, must be valid)
     * @param phone the client's phone (optional, validated if provided)
     * @param firstName the client's first name (required)
     * @param lastName the client's last name (required)
     * @throws IllegalArgumentException if any validation fails
     */
    public ClientInfo(String email, String phone, String firstName, String lastName) {
        validateEmail(email);
        validatePhone(phone);
        validateName(firstName, "First name");
        validateName(lastName, "Last name");

        this.email = email.toLowerCase().trim();
        this.phone = phone != null ? phone.trim() : null;
        this.firstName = firstName.trim();
        this.lastName = lastName.trim();
    }

    private void validateEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email is required");
        }
        if (!EMAIL_PATTERN.matcher(email.trim()).matches()) {
            throw new IllegalArgumentException("Invalid email format");
        }
    }

    private void validatePhone(String phone) {
        // Phone is optional, but if provided must be valid
        if (phone != null && !phone.isBlank()) {
            if (!PHONE_PATTERN.matcher(phone.trim()).matches()) {
                throw new IllegalArgumentException(
                    "Invalid phone format. Use international format (e.g., +1234567890)"
                );
            }
        }
    }

    private void validateName(String name, String fieldName) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        if (name.trim().length() < 2) {
            throw new IllegalArgumentException(fieldName + " must be at least 2 characters");
        }
        if (name.trim().length() > 100) {
            throw new IllegalArgumentException(fieldName + " cannot exceed 100 characters");
        }
    }

    /**
     * Returns the client's full name.
     *
     * @return "FirstName LastName"
     */
    public String fullName() {
        return firstName + " " + lastName;
    }

    /**
     * Returns a masked email for logging purposes (GDPR compliance).
     * Example: "j***@example.com"
     *
     * @return masked email
     */
    public String maskedEmail() {
        if (email == null || !email.contains("@")) {
            return "***";
        }
        String[] parts = email.split("@");
        String localPart = parts[0];
        String domain = parts[1];

        if (localPart.length() <= 2) {
            return "***@" + domain;
        }
        return localPart.substring(0, 1) + "***@" + domain;
    }

    /**
     * Returns a masked phone for logging purposes (GDPR compliance).
     * Example: "+1234***890"
     *
     * @return masked phone or null if no phone
     */
    public String maskedPhone() {
        if (phone == null || phone.length() < 6) {
            return phone;
        }
        int visibleDigits = 3;
        String start = phone.substring(0, visibleDigits);
        String end = phone.substring(phone.length() - visibleDigits);
        return start + "***" + end;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ClientInfo that = (ClientInfo) o;
        return Objects.equals(email, that.email) &&
               Objects.equals(phone, that.phone) &&
               Objects.equals(firstName, that.firstName) &&
               Objects.equals(lastName, that.lastName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(email, phone, firstName, lastName);
    }

    @Override
    public String toString() {
        // SECURITY: Never log actual email/phone, use masked versions
        return String.format("ClientInfo{email='%s', phone='%s', name='%s'}",
            maskedEmail(), maskedPhone(), fullName());
    }
}
