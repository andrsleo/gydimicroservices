package com.affiliate.rentals.gydi.properties.domain.model;

import java.time.LocalDate;

public record DateRange(LocalDate start, LocalDate end) {
    public boolean overlaps(DateRange other) {
        return !start.isAfter(other.end) && !end.isBefore(other.start);
    }
}
