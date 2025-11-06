package com.affiliate.rentals.gydi.properties.domain.exception;

import com.affiliate.rentals.gydi.shared.exception.HttpStatusMapping;
import org.springframework.http.HttpStatus;

/**
 * Exception thrown when a property is not found.
 *
 * @author GYDI Development Team
 */
@HttpStatusMapping(status = HttpStatus.NOT_FOUND, errorType = "Property Not Found")
public final class PropertyNotFoundException extends PropertyDomainException {

    /**
     * Creates exception for property not found by ID.
     *
     * @param propertyId the property ID that was not found
     */
    public PropertyNotFoundException(String propertyId) {
        super("Property not found with ID: " + propertyId);
    }
}