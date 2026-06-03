package com.affiliate.rentals.gydi.calendar.domain.exception;

import java.time.LocalDate;

public class CalendarDayBlockedException extends RuntimeException {

    public CalendarDayBlockedException(LocalDate date, Long propertyId) {
        super("Date " + date + " is already blocked for property " + propertyId);
    }
}
