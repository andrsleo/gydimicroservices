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
     */
    record CreateBookingCommand(
        Long referralLinkId,
        Long propertyId,
        LocalDate startDate,
        LocalDate endDate,
        String clientEmail,
        String clientPhone,
        String clientFirstName,
        String clientLastName,
        BigDecimal totalAmount,
        String currency
    ) {
        public CreateBookingCommand {
            if (referralLinkId == null) {
                throw new IllegalArgumentException("Referral link ID is required");
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
            if (totalAmount == null || totalAmount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Total amount must be positive");
            }
            if (currency == null || currency.isBlank()) {
                throw new IllegalArgumentException("Currency is required");
            }
        }
    }

    /**
     * Response after creating a booking.
     */
    record BookingResponse(
        Long bookingId,
        Long referralLinkId,
        Long propertyId,
        LocalDate startDate,
        LocalDate endDate,
        String clientEmail,
        String clientFullName,
        BigDecimal totalAmount,
        String currency,
        String status,
        String createdAt
    ) {}
}
