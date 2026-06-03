package com.affiliate.rentals.gydi.calendar.application.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SetCustomPriceRequest(
        @NotNull LocalDate date,
        @NotNull @DecimalMin("0.01") BigDecimal amount,
        @NotBlank String currency
) {}
