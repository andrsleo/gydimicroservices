package com.affiliate.rentals.gydi.commissions.domain.exception;

/**
 * Exception thrown when commission calculation fails.
 */
public class CommissionCalculationException extends CommissionDomainException {

    public CommissionCalculationException(String message) {
        super(message);
    }

    public CommissionCalculationException(String message, Throwable cause) {
        super(message, cause);
    }
}
