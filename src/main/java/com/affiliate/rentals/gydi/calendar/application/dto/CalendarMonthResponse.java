package com.affiliate.rentals.gydi.calendar.application.dto;

import java.util.List;

public record CalendarMonthResponse(
        Long propertyId,
        int year,
        int month,
        MoneyDto basePrice,
        SeasonPricingDto seasonPricing,
        List<CalendarDayDto> days
) {}
