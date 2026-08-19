package com.affiliate.rentals.gydi.commissions.domain.exception;

/**
 * Base exception for commission domain layer.
 * <p>
 * All domain exceptions inherit from this class.
 * NO framework dependencies (pure Java).
 * </p>
 */
public abstract class CommissionDomainException extends RuntimeException {

    protected CommissionDomainException(String message) {
        super(message);
    }

    protected CommissionDomainException(String message, Throwable cause) {
        super(message, cause);
    }
}
