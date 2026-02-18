package com.affiliate.rentals.gydi.commissions.domain.exception;

/**
 * Exception thrown when attempting an invalid state transition on a commission.
 */
public class InvalidCommissionStateException extends CommissionDomainException {

    public InvalidCommissionStateException(String message) {
        super(message);
    }

    public InvalidCommissionStateException(String message, Throwable cause) {
        super(message, cause);
    }
}
