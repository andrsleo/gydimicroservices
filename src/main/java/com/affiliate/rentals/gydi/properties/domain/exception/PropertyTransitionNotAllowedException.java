package com.affiliate.rentals.gydi.properties.domain.exception;

import com.affiliate.rentals.gydi.properties.domain.model.PropertyStatus;
import com.affiliate.rentals.gydi.shared.exception.HttpStatusMapping;
import org.springframework.http.HttpStatus;

/**
 * Exception thrown when an invalid property status transition is attempted.
 */
@HttpStatusMapping(status = HttpStatus.BAD_REQUEST, errorType = "Property Transition Not Allowed")
public class PropertyTransitionNotAllowedException extends RuntimeException {

    private final PropertyStatus fromStatus;
    private final PropertyStatus toStatus;

    public PropertyTransitionNotAllowedException(PropertyStatus fromStatus, PropertyStatus toStatus) {
        super(String.format("Cannot transition property from %s to %s", fromStatus, toStatus));
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
    }

    public PropertyStatus getFromStatus() {
        return fromStatus;
    }

    public PropertyStatus getToStatus() {
        return toStatus;
    }
}
