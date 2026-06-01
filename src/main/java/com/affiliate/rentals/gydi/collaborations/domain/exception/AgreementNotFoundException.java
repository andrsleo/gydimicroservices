package com.affiliate.rentals.gydi.collaborations.domain.exception;

public final class AgreementNotFoundException extends CollaborationDomainException {
    public AgreementNotFoundException(String message) { super(message); }
    public static AgreementNotFoundException withId(Long id) {
        return new AgreementNotFoundException("Agreement not found with ID: " + id);
    }
}
