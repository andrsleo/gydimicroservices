package com.affiliate.rentals.gydi.calendar.application.dto;

import java.util.List;

public record PriceBreakdownResponse(
        String checkIn,
        String checkOut,
        List<DayPriceDto> nights,
        MoneyDto total
) {
    public record DayPriceDto(
            String date,
            MoneyDto price,
            String priceSource,
            String status
    ) {}
}
