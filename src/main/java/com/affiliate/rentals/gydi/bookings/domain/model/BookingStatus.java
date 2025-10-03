package com.affiliate.rentals.gydi.bookings.domain.model;

/**
 * Booking status enumeration.
 * Using enum for type safety and compile-time validation.
 */
public enum BookingStatus {
    PENDING("PENDING"),
    CONFIRMED("CONFIRMED"),
    CANCELLED("CANCELLED"),
    COMPLETED("COMPLETED"),
    IN_PROGRESS("IN_PROGRESS");

    private final String value;

    BookingStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static BookingStatus fromValue(String value) {
        for (BookingStatus status : BookingStatus.values()) {
            if (status.value.equalsIgnoreCase(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Invalid booking status: " + value);
    }

    public boolean isActive() {
        return this == CONFIRMED || this == IN_PROGRESS;
    }

    public boolean isFinal() {
        return this == CANCELLED || this == COMPLETED;
    }

    public boolean canTransitionTo(BookingStatus newStatus) {
        return switch (this) {
            case PENDING -> newStatus == CONFIRMED || newStatus == CANCELLED;
            case CONFIRMED -> newStatus == IN_PROGRESS || newStatus == CANCELLED;
            case IN_PROGRESS -> newStatus == COMPLETED || newStatus == CANCELLED;
            case CANCELLED, COMPLETED -> false;
        };
    }
}