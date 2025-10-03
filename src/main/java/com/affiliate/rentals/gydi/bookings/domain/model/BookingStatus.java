package com.affiliate.rentals.gydi.bookings.domain.model;

/**
 * Enumeration of booking statuses in the rental system.
 *
 * <p>This enum provides type safety and compile-time validation for booking states.
 * It represents the lifecycle of a booking from initial creation through completion
 * or cancellation. The enum includes methods to determine valid state transitions
 * and categorize statuses (active, final, etc.).</p>
 *
 * <p>Booking lifecycle:</p>
 * <ul>
 *   <li>{@code PENDING} - Initial state, awaiting confirmation</li>
 *   <li>{@code CONFIRMED} - Booking confirmed, property reserved</li>
 *   <li>{@code IN_PROGRESS} - Guest has checked in, stay is ongoing</li>
 *   <li>{@code COMPLETED} - Stay finished successfully</li>
 *   <li>{@code CANCELLED} - Booking cancelled by guest or owner</li>
 * </ul>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * BookingStatus status = BookingStatus.PENDING;
 * boolean canConfirm = status.canTransitionTo(BookingStatus.CONFIRMED);
 * boolean isActive = status.isActive();
 * BookingStatus parsed = BookingStatus.fromValue("CONFIRMED");
 * }</pre>
 *
 * @author GYDI Development Team
 */
public enum BookingStatus {
    /**
     * Booking is pending confirmation.
     * This is the initial state for new bookings.
     */
    PENDING("PENDING"),

    /**
     * Booking has been confirmed.
     * The property is reserved for the specified dates.
     */
    CONFIRMED("CONFIRMED"),

    /**
     * Booking cancellation has been requested or processed.
     * No further actions can be taken on this booking.
     */
    CANCELLED("CANCELLED"),

    /**
     * Booking is complete and the stay has finished.
     * This is a final state.
     */
    COMPLETED("COMPLETED"),

    /**
     * Guest has checked in and the stay is currently in progress.
     */
    IN_PROGRESS("IN_PROGRESS");

    private final String value;

    /**
     * Constructs a BookingStatus with the specified string value.
     *
     * @param value the string representation of this booking status
     */
    BookingStatus(String value) {
        this.value = value;
    }

    /**
     * Returns the string value of this booking status.
     *
     * @return the booking status as a string
     */
    public String getValue() {
        return value;
    }

    /**
     * Converts a string value to its corresponding BookingStatus enum constant.
     * The comparison is case-insensitive.
     *
     * @param value the string value to convert
     * @return the matching BookingStatus
     * @throws IllegalArgumentException if the value doesn't match any booking status
     */
    public static BookingStatus fromValue(String value) {
        for (BookingStatus status : BookingStatus.values()) {
            if (status.value.equalsIgnoreCase(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Invalid booking status: " + value);
    }

    /**
     * Checks if this status represents an active booking.
     * Active bookings are those that are confirmed or in progress.
     *
     * @return {@code true} if the booking is active, {@code false} otherwise
     */
    public boolean isActive() {
        return this == CONFIRMED || this == IN_PROGRESS;
    }

    /**
     * Checks if this status represents a final state.
     * Final states are those where no further transitions are allowed.
     *
     * @return {@code true} if this is a final status, {@code false} otherwise
     */
    public boolean isFinal() {
        return this == CANCELLED || this == COMPLETED;
    }

    /**
     * Determines if a transition from this status to a new status is valid.
     * Enforces business rules for booking state transitions.
     *
     * <p>Valid transitions:</p>
     * <ul>
     *   <li>PENDING → CONFIRMED or CANCELLED</li>
     *   <li>CONFIRMED → IN_PROGRESS or CANCELLED</li>
     *   <li>IN_PROGRESS → COMPLETED or CANCELLED</li>
     *   <li>CANCELLED → (no transitions allowed)</li>
     *   <li>COMPLETED → (no transitions allowed)</li>
     * </ul>
     *
     * @param newStatus the target status to transition to
     * @return {@code true} if the transition is allowed, {@code false} otherwise
     */
    public boolean canTransitionTo(BookingStatus newStatus) {
        return switch (this) {
            case PENDING -> newStatus == CONFIRMED || newStatus == CANCELLED;
            case CONFIRMED -> newStatus == IN_PROGRESS || newStatus == CANCELLED;
            case IN_PROGRESS -> newStatus == COMPLETED || newStatus == CANCELLED;
            case CANCELLED, COMPLETED -> false;
        };
    }
}
