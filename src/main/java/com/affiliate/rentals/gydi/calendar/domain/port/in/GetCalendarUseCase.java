package com.affiliate.rentals.gydi.calendar.domain.port.in;

import com.affiliate.rentals.gydi.calendar.application.dto.CalendarMonthResponse;

public interface GetCalendarUseCase {
    CalendarMonthResponse execute(Long propertyId, int year, int month);
}
