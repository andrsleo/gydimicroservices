package com.affiliate.rentals.gydi.bookings.domain.model;

/**
 * Booking lifecycle status.
 * <p>
 * State transitions:
 * <pre>
 * REQUEST → RESERVED → FINISHED (completed successfully)
 * REQUEST → CANCELED (rejected or timeout)
 * RESERVED → CANCELED (canceled before completion)
 * </pre>
 */
public enum BookingStatus {
    /**
     * Initial state when client submits a booking quotation request.
     * Waiting for property owner confirmation.
     */
    REQUEST("Pending host confirmation"),

    /**
     * Booking confirmed by property owner.
     * Client can proceed with stay.
     */
    RESERVED("Confirmed by host"),

    /**
     * Booking completed successfully.
     * Client checked out and stay finished.
     * <p>
     * <strong>TRIGGER:</strong> Creates payment.booking record when transitioning to this state.
     * </p>
     */
    FINISHED("Booking completed"),

    /**
     * Booking canceled by client, owner, admin, or system.
     * Terminal state - no further transitions allowed.
     */
    CANCELED("Booking canceled");

    private final String description;

    BookingStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Validates if transition to target status is allowed.
     *
     * @param targetStatus the desired status to transition to
     * @return true if transition is valid
     */
    public boolean canTransitionTo(BookingStatus targetStatus) {
        return switch (this) {
            case REQUEST -> targetStatus == RESERVED || targetStatus == CANCELED;
            case RESERVED -> targetStatus == FINISHED || targetStatus == CANCELED;
            case FINISHED, CANCELED -> false; // Terminal states
        };
    }

    /**
     * Checks if this is a terminal state (no further transitions allowed).
     *
     * @return true if terminal state (FINISHED or CANCELED)
     */
    public boolean isTerminal() {
        return this == FINISHED || this == CANCELED;
    }

    /**
     * Checks if booking is active (can still be modified/processed).
     *
     * @return true if REQUEST or RESERVED
     */
    public boolean isActive() {
        return this == REQUEST || this == RESERVED;
    }
}
