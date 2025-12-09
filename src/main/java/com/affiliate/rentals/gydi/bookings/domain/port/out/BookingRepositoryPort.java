package com.affiliate.rentals.gydi.bookings.domain.port.out;

import com.affiliate.rentals.gydi.bookings.domain.model.Booking;
import com.affiliate.rentals.gydi.bookings.domain.model.DateRange;

import java.util.List;
import java.util.Optional;

/**
 * Output Port for Booking persistence.
 * <p>
 * Implemented by: BookingRepositoryAdapter (infrastructure layer)
 * </p>
 */
public interface BookingRepositoryPort {

    /**
     * Saves a booking (create or update).
     *
     * @param booking the booking to save
     * @return the saved booking with ID assigned
     */
    Booking save(Booking booking);

    /**
     * Finds a booking by its ID.
     *
     * @param id the booking ID
     * @return Optional containing the booking if found
     */
    Optional<Booking> findById(Long id);

    /**
     * Finds all bookings for a specific referral link.
     *
     * @param referralLinkId the referral link ID
     * @return list of bookings
     */
    List<Booking> findByReferralLinkId(Long referralLinkId);

    /**
     * Checks if property is available for given date range.
     * <p>
     * A property is available if there are no RESERVED or FINISHED bookings
     * that overlap with the requested dates.
     * </p>
     *
     * @param propertyId the property ID
     * @param dateRange the requested date range
     * @return true if property is available
     */
    boolean isPropertyAvailable(Long propertyId, DateRange dateRange);

    /**
     * Finds all active bookings (REQUEST or RESERVED) for a property.
     *
     * @param propertyId the property ID
     * @return list of active bookings
     */
    List<Booking> findActiveBookingsByProperty(Long propertyId);

    /**
     * Finds all bookings by status.
     *
     * @param status the booking status
     * @return list of bookings
     */
    List<Booking> findByStatus(String status);

    /**
     * Deletes a booking (should rarely be used, prefer status changes).
     *
     * @param id the booking ID
     */
    void deleteById(Long id);
}
