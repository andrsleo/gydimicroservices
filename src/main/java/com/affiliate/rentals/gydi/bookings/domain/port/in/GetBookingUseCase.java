package com.affiliate.rentals.gydi.bookings.domain.port.in;

import java.util.List;

/**
 * Input Port for querying bookings.
 */
public interface GetBookingUseCase {

    /**
     * Gets a booking by its ID.
     *
     * @param bookingId the booking ID
     * @return the booking response
     */
    CreateBookingUseCase.BookingResponse getById(Long bookingId);

    /**
     * Gets all bookings for a referral link.
     *
     * @param referralLinkId the referral link ID
     * @return list of bookings
     */
    List<CreateBookingUseCase.BookingResponse> getByReferralLinkId(Long referralLinkId);

    /**
     * Gets all active bookings for a property.
     *
     * @param propertyId the property ID
     * @return list of active bookings
     */
    List<CreateBookingUseCase.BookingResponse> getActiveBookingsByProperty(Long propertyId);
}
