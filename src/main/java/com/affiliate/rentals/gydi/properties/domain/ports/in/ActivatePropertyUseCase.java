package com.affiliate.rentals.gydi.properties.domain.ports.in;

import com.affiliate.rentals.gydi.properties.domain.model.Property;
import com.affiliate.rentals.gydi.properties.domain.model.PropertyId;

/**
 * Use case for activating an inactive property (making it published again).
 */
public interface ActivatePropertyUseCase {

    /**
     * Activates an inactive property.
     *
     * @param command the activate command
     * @return the activated Property
     */
    Property activateProperty(ActivatePropertyCommand command);

    /**
     * Command for activating a property
     */
    record ActivatePropertyCommand(
        PropertyId propertyId,
        Long requestingUserId
    ) {}
}