package com.affiliate.rentals.gydi.bookings.domain.model;

/**
 * Enumeration of reasons for blocking property availability.
 *
 * <p>This enum provides type safety and compile-time validation for availability block reasons.
 * It represents various scenarios where a property date should be marked as unavailable,
 * including actual bookings, maintenance work, owner personal use, or manual blocks.</p>
 *
 * <p>Block reasons:</p>
 * <ul>
 *   <li>{@code BOOKING} - Date is blocked due to an active booking</li>
 *   <li>{@code MAINTENANCE} - Property undergoing maintenance or repairs</li>
 *   <li>{@code OWNER_USE} - Owner is using the property personally</li>
 *   <li>{@code BLOCKED} - Manually blocked by owner for any other reason</li>
 * </ul>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * BlockReason reason = BlockReason.BOOKING;
 * boolean isBooking = reason.isBooking();
 * boolean canOverride = reason.canBeOverridden();
 * BlockReason parsed = BlockReason.fromValue("MAINTENANCE");
 * }</pre>
 *
 * @author GYDI Development Team
 * @see PropertyAvailability
 */
public enum BlockReason {
    /**
     * Date is blocked due to an active booking.
     * This block is created automatically when a booking is confirmed.
     */
    BOOKING("BOOKING"),

    /**
     * Property is unavailable due to maintenance or repairs.
     * Can be overridden if necessary.
     */
    MAINTENANCE("MAINTENANCE"),

    /**
     * Property is reserved for owner's personal use.
     * Cannot be overridden for bookings.
     */
    OWNER_USE("OWNER_USE"),

    /**
     * Property is manually blocked by the owner.
     * Can be overridden by the owner if needed.
     */
    BLOCKED("BLOCKED");

    private final String value;

    /**
     * Constructs a BlockReason with the specified string value.
     *
     * @param value the string representation of this block reason
     */
    BlockReason(String value) {
        this.value = value;
    }

    /**
     * Returns the string value of this block reason.
     *
     * @return the block reason as a string
     */
    public String getValue() {
        return value;
    }

    /**
     * Converts a string value to its corresponding BlockReason enum constant.
     * The comparison is case-insensitive.
     *
     * @param value the string value to convert
     * @return the matching BlockReason
     * @throws IllegalArgumentException if the value doesn't match any block reason
     */
    public static BlockReason fromValue(String value) {
        for (BlockReason reason : BlockReason.values()) {
            if (reason.value.equalsIgnoreCase(value)) {
                return reason;
            }
        }
        throw new IllegalArgumentException("Invalid block reason: " + value);
    }

    /**
     * Checks if this block reason represents a booking.
     *
     * @return {@code true} if the reason is BOOKING, {@code false} otherwise
     */
    public boolean isBooking() {
        return this == BOOKING;
    }

    /**
     * Checks if this block reason can be overridden by the owner.
     * Blocks for bookings and owner use cannot be overridden, while
     * maintenance and manual blocks can be removed if needed.
     *
     * @return {@code true} if the block can be overridden, {@code false} otherwise
     */
    public boolean canBeOverridden() {
        return this == BLOCKED || this == MAINTENANCE;
    }
}
