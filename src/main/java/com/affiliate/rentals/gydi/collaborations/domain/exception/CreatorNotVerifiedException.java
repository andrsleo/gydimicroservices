package com.affiliate.rentals.gydi.collaborations.domain.exception;

public final class CreatorNotVerifiedException extends CollaborationDomainException {
    public CreatorNotVerifiedException() {
        super("Only verified creators can submit pitches");
    }
}
