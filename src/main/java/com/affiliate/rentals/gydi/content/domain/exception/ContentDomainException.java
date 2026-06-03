package com.affiliate.rentals.gydi.content.domain.exception;

/**
 * Base exception for all domain-level exceptions in the content bounded context.
 *
 * @author GYDI Development Team
 */
public abstract class ContentDomainException extends RuntimeException {

    protected ContentDomainException(String message) {
        super(message);
    }

    protected ContentDomainException(String message, Throwable cause) {
        super(message, cause);
    }
}
