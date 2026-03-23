package com.affiliate.rentals.gydi.bookings.domain.ports;

import com.affiliate.rentals.gydi.bookings.domain.model.Booking;
import com.affiliate.rentals.gydi.bookings.domain.model.BookingStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository Port for Booking aggregate.
 * <p>
 * Defines persistence operations without infrastructure dependencies.
 * Implemented by infrastructure layer (BookingRepositoryAdapter).
 * </p>
 */
public interface BookingRepositoryPort {

    /**
     * Saves a booking (create or update).
     *
     * @param booking the booking to save
     * @return the saved booking with assigned ID
     */
    Booking save(Booking booking);

    /**
     * Finds a booking by its ID.
     *
     * @param bookingId the booking ID
     * @return Optional containing the booking if found
     */
    Optional<Booking> findById(Long bookingId);

    /**
     * Finds all bookings for a specific property.
     *
     * @param propertyId the property ID
     * @return list of bookings for the property
     */
    List<Booking> findByPropertyId(Long propertyId);

    /**
     * Finds all bookings for a specific referral link.
     *
     * @param referralLinkId the referral link ID
     * @return list of bookings for the referral link
     */
    List<Booking> findByReferralLinkId(Long referralLinkId);

    /**
     * Finds all bookings with a specific status.
     *
     * @param status the booking status
     * @return list of bookings with the status
     */
    List<Booking> findByStatus(BookingStatus status);

    /**
     * Finds all bookings that should transition to IN_PROGRESS.
     * <p>
     * Bookings in RESERVED status where check-in date is today or past.
     * </p>
     *
     * @param currentDate the current date
     * @return list of bookings ready to start
     */
    List<Booking> findReadyToStart(LocalDate currentDate);

    /**
     * Finds all bookings that should transition to FINISHED.
     * <p>
     * Bookings in IN_PROGRESS status where check-out date is today or past.
     * </p>
     *
     * @param currentDate the current date
     * @return list of bookings ready to finish
     */
    List<Booking> findReadyToFinish(LocalDate currentDate);

    /**
     * Finds all bookings (for admin dashboard).
     *
     * @return list of all bookings
     */
    List<Booking> findAll();

    /**
     * Deletes a booking by its ID.
     * <p>
     * Note: Should rarely be used. Prefer cancellation over deletion.
     * </p>
     *
     * @param bookingId the booking ID
     */
    void deleteById(Long bookingId);

    /**
     * Checks if a booking exists.
     *
     * @param bookingId the booking ID
     * @return true if booking exists
     */
    boolean existsById(Long bookingId);

    /**
     * Checks if there are any overlapping bookings for a property within the
     * specified date range.
     * <p>
     * ✅ SECURITY FIX: Prevents double-booking attacks.
     * </p>
     * <p>
     * A booking overlaps if:
     * - Its check-in is before the requested check-out AND
     * - Its check-out is after the requested check-in
     * </p>
     * <p>
     * Only considers bookings in active states (not CANCELLED or FINISHED).
     * </p>
     *
     * @param propertyId   the property ID
     * @param checkInDate  the requested check-in date
     * @param checkOutDate the requested check-out date
     * @return true if there are overlapping bookings
     */
    boolean existsOverlappingBooking(Long propertyId, LocalDate checkInDate, LocalDate checkOutDate);

    /**
     * Finds the host ID associated with a property.
     * <p>
     * Used to fetch host subscription details for commission calculation.
     * </p>
     *
     * @param propertyId the property ID
     * @return Optional containing the host ID if property exists
     */
    Optional<Long> findHostIdByPropertyId(Long propertyId);

    /**
     * Finds the affiliate ID associated with a referral link.
     *
     * @param referralLinkId the referral link ID
     * @return Optional containing the affiliate ID if link exists
     */
    Optional<Long> findAffiliateIdByReferralLinkId(Long referralLinkId);

    /**
     * Finds the user ID of the system organic user (system-organic@gydi.internal).
     * <p>
     * Used to detect organic bookings (no real affiliate) so that commission
     * rates can be applied correctly: affiliate commission = 0%, host fee = 15%.
     * </p>
     *
     * @return Optional containing the organic system user ID if found
     */
    Optional<Long> findOrganicSystemUserId();
}
