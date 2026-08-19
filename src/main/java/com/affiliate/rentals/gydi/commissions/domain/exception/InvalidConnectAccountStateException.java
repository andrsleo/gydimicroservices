package com.affiliate.rentals.gydi.commissions.domain.exception;

/**
 * Exception thrown when attempting an invalid state transition on a Stripe Connect Account.
 */
public class InvalidConnectAccountStateException extends RuntimeException {

    public InvalidConnectAccountStateException(String message) {
        super(message);
    }

    public InvalidConnectAccountStateException(String message, Throwable cause) {
        super(message, cause);
    }
}
