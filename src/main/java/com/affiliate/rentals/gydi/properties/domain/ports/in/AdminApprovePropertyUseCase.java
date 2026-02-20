package com.affiliate.rentals.gydi.properties.domain.ports.in;

import com.affiliate.rentals.gydi.properties.domain.model.Property;
import com.affiliate.rentals.gydi.properties.domain.model.PropertyId;

/**
 * Use case for admin approving a pending property.
 */
public interface AdminApprovePropertyUseCase {

    /**
     * Approves a property that is pending admin review.
     *
     * @param command the approve command
     * @return the approved Property in PUBLISHED status
     */
    Property approveProperty(ApprovePropertyCommand command);

    /**
     * Command for approving a property
     */
    record ApprovePropertyCommand(
            PropertyId propertyId,
            Long adminUserId) {
    }
}
