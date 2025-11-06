package com.affiliate.rentals.gydi.properties.domain.ports.in;

import com.affiliate.rentals.gydi.properties.domain.model.Property;
import com.affiliate.rentals.gydi.properties.domain.model.PropertyId;

/**
 * Use case for deactivating a published property (making it temporarily inactive).
 */
public interface DeactivatePropertyUseCase {

    /**
     * Deactivates a published property.
     *
     * @param command the deactivate command
     * @return the deactivated Property
     */
    Property deactivateProperty(DeactivatePropertyCommand command);

    /**
     * Command for deactivating a property
     */
    record DeactivatePropertyCommand(
        PropertyId propertyId,
        Long requestingUserId
    ) {}
}