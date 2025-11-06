package com.affiliate.rentals.gydi.properties.domain.ports.in;

import com.affiliate.rentals.gydi.properties.domain.model.PropertyId;

/**
 * Use case for deleting a property (soft delete).
 */
public interface DeletePropertyUseCase {
    
    /**
     * Deletes a property (soft delete).
     *
     * @param command the delete command
     */
    void deleteProperty(DeletePropertyCommand command);
    
    /**
     * Command for deleting a property
     */
    record DeletePropertyCommand(
        PropertyId propertyId,
        Long requestingUserId
    ) {}
}
