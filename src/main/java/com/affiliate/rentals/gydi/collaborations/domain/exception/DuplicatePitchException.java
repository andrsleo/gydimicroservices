package com.affiliate.rentals.gydi.collaborations.domain.exception;

public final class DuplicatePitchException extends CollaborationDomainException {
    public DuplicatePitchException(Long creatorId, Long propertyId) {
        super("Creator " + creatorId + " already has an active pitch for property " + propertyId);
    }
}
