package com.affiliate.rentals.gydi.calendar.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UpdateSeasonDefinitionRequest(

    @NotBlank(message = "name is required")
    String name,

    @NotBlank(message = "seasonType is required")
    @Pattern(regexp = "HIGH|MEDIUM|LOW", message = "seasonType must be HIGH, MEDIUM, or LOW")
    String seasonType,

    @NotBlank(message = "scope is required")
    @Pattern(regexp = "GLOBAL|REGION|COUNTRY|SUBREGION",
             message = "scope must be GLOBAL, REGION, COUNTRY, or SUBREGION")
    String scope,

    String country,
    String regionCode,
    String region,

    @NotBlank(message = "startDate is required")
    String startDate,

    @NotBlank(message = "endDate is required")
    String endDate,

    @NotBlank(message = "recurrence is required")
    @Pattern(regexp = "NONE|YEARLY", message = "recurrence must be NONE or YEARLY")
    String recurrence
) {}
