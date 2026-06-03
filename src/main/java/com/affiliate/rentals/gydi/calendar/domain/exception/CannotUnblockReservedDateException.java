package com.affiliate.rentals.gydi.calendar.domain.exception;

import java.time.LocalDate;

public class CannotUnblockReservedDateException extends RuntimeException {

    public CannotUnblockReservedDateException(LocalDate date, Long propertyId) {
        super("Cannot unblock date " + date + " for property " + propertyId
                + ": it is reserved by an active booking");
    }
}
