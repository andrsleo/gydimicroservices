package com.affiliate.rentals.gydi.calendar.application.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

public record BlockDatesRequest(
        @NotEmpty List<@NotNull LocalDate> dates,
        String notes
) {}
