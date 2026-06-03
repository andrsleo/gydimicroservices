package com.affiliate.rentals.gydi.calendar.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record CreateSeasonDefinitionRequest(

    @NotBlank(message = "name is required")
    String name,

    @NotBlank(message = "seasonType is required")
    @Pattern(regexp = "HIGH|MEDIUM|LOW", message = "seasonType must be HIGH, MEDIUM, or LOW")
    String seasonType,

    @NotBlank(message = "scope is required")
    @Pattern(regexp = "GLOBAL|REGION|COUNTRY|SUBREGION",
             message = "scope must be GLOBAL, REGION, COUNTRY, or SUBREGION")
    String scope,

    /** Required when scope = COUNTRY or SUBREGION. */
    String country,

    /** Required when scope = REGION. Must match a season_region.code. */
    String regionCode,

    /** Optional sub-national region name (when scope = SUBREGION). */
    String region,

    @NotBlank(message = "startDate is required (ISO format: yyyy-MM-dd)")
    String startDate,

    @NotBlank(message = "endDate is required (ISO format: yyyy-MM-dd)")
    String endDate,

    @NotBlank(message = "recurrence is required")
    @Pattern(regexp = "NONE|YEARLY", message = "recurrence must be NONE or YEARLY")
    String recurrence
) {}
