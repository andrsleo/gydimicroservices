package com.affiliate.rentals.gydi.calendar.domain.exception;

import java.time.LocalDate;

public class DateNotBlockedException extends RuntimeException {

    public DateNotBlockedException(LocalDate date, Long propertyId) {
        super("Date " + date + " is not blocked for property " + propertyId);
    }
}
