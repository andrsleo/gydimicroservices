package com.affiliate.rentals.gydi.collaborations.domain.exception;

public final class MonthlyPitchLimitException extends CollaborationDomainException {
    public MonthlyPitchLimitException() {
        super("Monthly pitch limit reached (10 pitches per calendar month)");
    }
}
