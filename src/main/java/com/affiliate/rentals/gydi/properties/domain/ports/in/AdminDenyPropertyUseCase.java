package com.affiliate.rentals.gydi.properties.domain.ports.in;

import com.affiliate.rentals.gydi.properties.domain.model.Property;
import com.affiliate.rentals.gydi.properties.domain.model.PropertyId;

/**
 * Use case for admin denying a pending property with a reason.
 */
public interface AdminDenyPropertyUseCase {

    /**
     * Denies a property that is pending admin review.
     *
     * @param command the deny command
     * @return the denied Property in DENY status
     */
    Property denyProperty(DenyPropertyCommand command);

    /**
     * Command for denying a property
     */
    record DenyPropertyCommand(
            PropertyId propertyId,
            Long adminUserId,
            String reason) {
    }
}
