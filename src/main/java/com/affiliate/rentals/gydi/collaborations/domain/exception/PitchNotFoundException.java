package com.affiliate.rentals.gydi.collaborations.domain.exception;

public final class PitchNotFoundException extends CollaborationDomainException {
    public PitchNotFoundException(String message) { super(message); }
    public static PitchNotFoundException withId(Long id) {
        return new PitchNotFoundException("Pitch not found with ID: " + id);
    }
}
