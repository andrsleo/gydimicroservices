package com.affiliate.rentals.gydi.properties.domain.ports.in;

import com.affiliate.rentals.gydi.properties.domain.model.Property;
import com.affiliate.rentals.gydi.properties.domain.model.PropertyId;

/**
 * Use case for submitting a property for admin approval.
 */
public interface SubmitForApprovalUseCase {

    /**
     * Submits a property for admin review.
     *
     * @param command the submit command
     * @return the updated Property in PENDING_APPROVAL status
     */
    Property submitForApproval(SubmitForApprovalCommand command);

    /**
     * Command for submitting a property for approval
     */
    record SubmitForApprovalCommand(
            PropertyId propertyId,
            Long requestingUserId) {
    }
}
