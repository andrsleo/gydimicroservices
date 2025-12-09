package com.affiliate.rentals.gydi.bookings.domain.port.in;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Input Port for creating a booking request.
 * <p>
 * Implemented by: CreateBookingUseCaseImpl (application layer)
 * </p>
 */
public interface CreateBookingUseCase {

    /**
     * Creates a new booking in REQUEST status.
     *
     * @param command the booking creation command
     * @return the created booking response
     */
    BookingResponse execute(CreateBookingCommand command);

    /**
     * Command to create a booking.
     * Financial details (totalAmount, currency) removed - tracked separately in commission_booking table.
     */
    record CreateBookingCommand(
            String referralLinkId, // Can be either numeric ID or JWT token
            Long propertyId,
            LocalDate startDate,
            LocalDate endDate,
            String clientEmail,
            String clientPhone,
            String clientFirstName,
            String clientLastName) {
        public CreateBookingCommand {
            if (referralLinkId == null || referralLinkId.isBlank()) {
                throw new IllegalArgumentException("Referral link ID or token is required");
            }
            if (propertyId == null) {
                throw new IllegalArgumentException("Property ID is required");
            }
            if (startDate == null || endDate == null) {
                throw new IllegalArgumentException("Start and end dates are required");
            }
            if (clientEmail == null || clientEmail.isBlank()) {
                throw new IllegalArgumentException("Client email is required");
            }
            if (clientFirstName == null || clientFirstName.isBlank()) {
                throw new IllegalArgumentException("Client first name is required");
            }
            if (clientLastName == null || clientLastName.isBlank()) {
                throw new IllegalArgumentException("Client last name is required");
            }
        }
    }

    /**
     * Response after creating a booking.
     * Financial details (totalAmount, currency) removed - tracked separately in commission_booking table.
     */
    record BookingResponse(
            Long bookingId,
            Long referralLinkId,
            Long propertyId,
            LocalDate startDate,
            LocalDate endDate,
            String clientEmail,
            String clientFullName,
            String status,
            String createdAt) {
    }
}
