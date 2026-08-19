package com.affiliate.rentals.gydi.bookings.domain.model.vo;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

/**
 * Value Object representing booking check-in and check-out dates.
 * <p>
 * Immutable and validates business rules:
 * - Check-in must be in the future (not today or past)
 * - Check-out must be after check-in
 * - Minimum stay of 1 night
 * - Maximum stay of 30 days
 * </p>
 *
 * ✅ SECURITY FIX: Prevents invalid booking dates and potential abuse.
 */
public final class BookingDates {
    private static final int MAX_STAY_DAYS = 30;

    private final LocalDate checkInDate;
    private final LocalDate checkOutDate;

    private BookingDates(LocalDate checkInDate, LocalDate checkOutDate) {
        this.checkInDate = checkInDate;
        this.checkOutDate = checkOutDate;
    }

    /**
     * Creates BookingDates with validation.
     *
     * @param checkInDate guest check-in date (required)
     * @param checkOutDate guest check-out date (required, must be after check-in)
     * @return validated BookingDates
     * @throws IllegalArgumentException if validation fails
     */
    public static BookingDates of(LocalDate checkInDate, LocalDate checkOutDate) {
        validateDates(checkInDate, checkOutDate);
        return new BookingDates(checkInDate, checkOutDate);
    }

    /**
     * Reconstitutes BookingDates from persistence without future date validation.
     * <p>
     * Use this method ONLY when loading existing bookings from the database.
     * For creating new bookings, use {@link #of(LocalDate, LocalDate)} instead.
     * </p>
     *
     * @param checkInDate guest check-in date (required)
     * @param checkOutDate guest check-out date (required, must be after check-in)
     * @return reconstituted BookingDates
     * @throws IllegalArgumentException if dates are null or check-out is not after check-in
     */
    public static BookingDates reconstitute(LocalDate checkInDate, LocalDate checkOutDate) {
        validateDatesForReconstitution(checkInDate, checkOutDate);
        return new BookingDates(checkInDate, checkOutDate);
    }

    private static void validateDates(LocalDate checkIn, LocalDate checkOut) {
        if (checkIn == null) {
            throw new IllegalArgumentException("Check-in date is required");
        }
        if (checkOut == null) {
            throw new IllegalArgumentException("Check-out date is required");
        }

        // ✅ SECURITY FIX: Check-in must be in the FUTURE (not today or past)
        LocalDate today = LocalDate.now();
        if (!checkIn.isAfter(today)) {
            throw new IllegalArgumentException(
                String.format("Check-in date (%s) must be in the future. Today is %s", checkIn, today)
            );
        }

        // Existing validation: Check-out must be after check-in (minimum 1 night)
        if (!checkOut.isAfter(checkIn)) {
            throw new IllegalArgumentException(
                String.format("Check-out date (%s) must be after check-in date (%s)", checkOut, checkIn)
            );
        }

        // ✅ SECURITY FIX: Maximum stay duration (prevents abuse)
        long numberOfNights = ChronoUnit.DAYS.between(checkIn, checkOut);
        if (numberOfNights > MAX_STAY_DAYS) {
            throw new IllegalArgumentException(
                String.format("Booking duration (%d nights) exceeds maximum allowed stay of %d days",
                    numberOfNights, MAX_STAY_DAYS)
            );
        }
    }

    /**
     * Validates dates for reconstitution (loading from database).
     * <p>
     * Skips future date validation since historical bookings may have past dates.
     * Only validates basic constraints: non-null dates and check-out after check-in.
     * </p>
     */
    private static void validateDatesForReconstitution(LocalDate checkIn, LocalDate checkOut) {
        if (checkIn == null) {
            throw new IllegalArgumentException("Check-in date is required");
        }
        if (checkOut == null) {
            throw new IllegalArgumentException("Check-out date is required");
        }

        // Only validate check-out is after check-in (allows past dates)
        if (!checkOut.isAfter(checkIn)) {
            throw new IllegalArgumentException(
                String.format("Check-out date (%s) must be after check-in date (%s)", checkOut, checkIn)
            );
        }
    }

    /**
     * Calculates the number of nights for this booking.
     *
     * @return number of nights (minimum 1)
     */
    public long getNumberOfNights() {
        return ChronoUnit.DAYS.between(checkInDate, checkOutDate);
    }

    /**
     * Checks if check-in date is today or in the past.
     *
     * @return true if check-in is today or earlier
     */
    public boolean isCheckInDue() {
        return !checkInDate.isAfter(LocalDate.now());
    }

    /**
     * Checks if check-out date is today or in the past.
     *
     * @return true if check-out is today or earlier
     */
    public boolean isCheckOutDue() {
        return !checkOutDate.isAfter(LocalDate.now());
    }

    // Getters
    public LocalDate getCheckInDate() {
        return checkInDate;
    }

    public LocalDate getCheckOutDate() {
        return checkOutDate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BookingDates that = (BookingDates) o;
        return Objects.equals(checkInDate, that.checkInDate) &&
               Objects.equals(checkOutDate, that.checkOutDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(checkInDate, checkOutDate);
    }

    @Override
    public String toString() {
        return String.format("BookingDates{%s to %s (%d nights)}",
            checkInDate, checkOutDate, getNumberOfNights());
    }
}
