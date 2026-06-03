package com.affiliate.rentals.gydi.content.domain.exception;

/**
 * Exception thrown when a requested content post cannot be found.
 *
 * @author GYDI Development Team
 */
public final class ContentNotFoundException extends ContentDomainException {

    public ContentNotFoundException(String message) {
        super(message);
    }

    public static ContentNotFoundException withId(Long id) {
        return new ContentNotFoundException("Contenido no encontrado con ID: " + id);
    }
}
