package com.affiliate.rentals.gydi.calendar.domain.port.in;

import com.affiliate.rentals.gydi.calendar.application.dto.PriceBreakdownResponse;

import java.time.LocalDate;

public interface CalculatePriceUseCase {
    PriceBreakdownResponse execute(Long propertyId, LocalDate checkIn, LocalDate checkOut);
}
