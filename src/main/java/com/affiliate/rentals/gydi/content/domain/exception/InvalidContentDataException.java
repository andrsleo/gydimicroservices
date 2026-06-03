package com.affiliate.rentals.gydi.content.domain.exception;

/**
 * Exception thrown when content post data violates business rules.
 *
 * @author GYDI Development Team
 */
public final class InvalidContentDataException extends ContentDomainException {

    public InvalidContentDataException(String message) {
        super(message);
    }
}
