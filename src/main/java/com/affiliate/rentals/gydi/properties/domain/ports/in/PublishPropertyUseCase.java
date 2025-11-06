package com.affiliate.rentals.gydi.properties.domain.ports.in;

import com.affiliate.rentals.gydi.properties.domain.model.Property;
import com.affiliate.rentals.gydi.properties.domain.model.PropertyId;

/**
 * Use case for publishing a property (making it visible in catalog).
 */
public interface PublishPropertyUseCase {
    
    /**
     * Publishes a property.
     *
     * @param command the publish command
     * @return the published Property
     */
    Property publishProperty(PublishPropertyCommand command);
    
    /**
     * Command for publishing a property
     */
    record PublishPropertyCommand(
        PropertyId propertyId,
        Long requestingUserId
    ) {}
}
