package com.affiliate.rentals.gydi.bookings.domain.model;

/**
 * Booking lifecycle status.
 * <p>
 * State transitions:
 * <pre>
 * REQUEST → RESERVED → IN_PROGRESS → FINISHED (completed successfully)
 * REQUEST → CANCELLED (rejected or timeout)
 * RESERVED → CANCELLED (cancelled before check-in)
 * IN_PROGRESS → CANCELLED (cancelled during stay)
 * FINISHED → DISPUTED (complaint or issue after completion)
 * </pre>
 */
public enum BookingStatus {
    /**
     * Initial state when guest submits a booking request via referral link.
     * Waiting for ADMIN confirmation in Airbnb.
     */
    REQUEST("Pending admin confirmation"),

    /**
     * Booking confirmed by ADMIN in Airbnb.
     * Guest has a confirmed reservation.
     */
    RESERVED("Confirmed by admin"),

    /**
     * Guest has checked in (automatic on check_in_date).
     * Stay is currently in progress.
     */
    IN_PROGRESS("Guest checked in"),

    /**
     * Booking completed successfully (automatic on check_out_date).
     * Guest checked out and stay finished.
     * <p>
     * <strong>TRIGGER:</strong> Creates commission record when transitioning to this state.
     * </p>
     */
    FINISHED("Booking completed"),

    /**
     * Booking cancelled by guest, host, or admin.
     * Can occur at any non-terminal stage.
     */
    CANCELLED("Booking cancelled"),

    /**
     * Issue or complaint after completion.
     * Allows commission adjustment or refund processing.
     */
    DISPUTED("Under dispute");

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
            case REQUEST -> targetStatus == RESERVED || targetStatus == CANCELLED;
            case RESERVED -> targetStatus == IN_PROGRESS || targetStatus == CANCELLED;
            case IN_PROGRESS -> targetStatus == FINISHED || targetStatus == CANCELLED;
            case FINISHED -> targetStatus == DISPUTED;
            case CANCELLED, DISPUTED -> false; // Terminal states
        };
    }

    /**
     * Checks if this is a terminal state (no further transitions allowed).
     *
     * @return true if terminal state (CANCELLED or DISPUTED)
     */
    public boolean isTerminal() {
        return this == CANCELLED || this == DISPUTED;
    }

    /**
     * Checks if booking is active (can still be modified/processed).
     *
     * @return true if REQUEST, RESERVED, or IN_PROGRESS
     */
    public boolean isActive() {
        return this == REQUEST || this == RESERVED || this == IN_PROGRESS;
    }

    /**
     * Checks if booking can be automatically progressed by scheduled job.
     *
     * @return true if automatic transition is possible
     */
    public boolean canAutoProgress() {
        return this == RESERVED || this == IN_PROGRESS;
    }
}
