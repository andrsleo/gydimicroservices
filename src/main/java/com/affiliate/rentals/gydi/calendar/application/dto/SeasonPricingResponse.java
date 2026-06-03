package com.affiliate.rentals.gydi.calendar.application.dto;

public record SeasonPricingResponse(
    Long propertyId,
    Double highMultiplier,
    Double mediumMultiplier,
    Double lowMultiplier
) {}
