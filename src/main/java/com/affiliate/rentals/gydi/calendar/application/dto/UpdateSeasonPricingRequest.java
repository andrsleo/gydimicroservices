package com.affiliate.rentals.gydi.calendar.application.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record UpdateSeasonPricingRequest(
    @NotNull @DecimalMin("0.10") @DecimalMax("10.00") BigDecimal highMultiplier,
    @NotNull @DecimalMin("0.10") @DecimalMax("10.00") BigDecimal mediumMultiplier,
    @NotNull @DecimalMin("0.10") @DecimalMax("10.00") BigDecimal lowMultiplier
) {}
