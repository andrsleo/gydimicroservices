package com.affiliate.rentals.gydi.content.domain.exception;

/**
 * Exception thrown when a user tries to modify content they do not own.
 *
 * @author GYDI Development Team
 */
public final class ContentAccessDeniedException extends ContentDomainException {

    public ContentAccessDeniedException(String message) {
        super(message);
    }
}
