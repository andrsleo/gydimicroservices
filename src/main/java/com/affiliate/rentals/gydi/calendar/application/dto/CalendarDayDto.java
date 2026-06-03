package com.affiliate.rentals.gydi.calendar.application.dto;

public record CalendarDayDto(
        String date,
        String status,
        Double effectivePrice,
        String effectivePriceCurrency,
        String priceSource,
        Double customPrice,
        String blockSource,
        Long bookingId
) {}
