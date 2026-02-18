package com.affiliate.rentals.gydi.commissions.domain.exception;

/**
 * Exception thrown when a commission is not found.
 */
public class CommissionNotFoundException extends CommissionDomainException {

    public CommissionNotFoundException(String message) {
        super(message);
    }

    public static CommissionNotFoundException forId(Long commissionId) {
        return new CommissionNotFoundException(
            "Commission not found with ID: " + commissionId
        );
    }

    public static CommissionNotFoundException forBooking(Long bookingId) {
        return new CommissionNotFoundException(
            "Commission not found for booking ID: " + bookingId
        );
    }
}
