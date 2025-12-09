package com.affiliate.rentals.gydi.properties.application.dto;

import java.time.LocalDate;

public record PropertyCalendarBlockResponse(
        LocalDate startDate,
        LocalDate endDate,
        String source) {
}
