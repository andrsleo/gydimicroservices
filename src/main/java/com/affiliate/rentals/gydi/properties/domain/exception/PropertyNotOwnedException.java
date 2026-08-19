package com.affiliate.rentals.gydi.properties.domain.exception;

import com.affiliate.rentals.gydi.shared.exception.HttpStatusMapping;
import org.springframework.http.HttpStatus;

/**
 * Exception thrown when a user attempts to access a property they don't own.
 *
 * @author GYDI Development Team
 */
@HttpStatusMapping(status = HttpStatus.FORBIDDEN, errorType = "Property Access Denied")
public final class PropertyNotOwnedException extends PropertyDomainException {

    /**
     * Creates exception for unauthorized property access.
     *
     * @param propertyId the property ID
     * @param userId     the user ID attempting access
     */
    public PropertyNotOwnedException(String propertyId, Long userId) {
        super(String.format("User %d does not own property %s", userId, propertyId));
    }
}