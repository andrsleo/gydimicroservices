package com.affiliate.rentals.gydi.calendar.application.dto;

public record SeasonDefinitionDto(
    Long id,
    String name,
    String seasonType,   // HIGH | MEDIUM | LOW
    String scope,        // GLOBAL | REGION | COUNTRY | SUBREGION
    String country,      // nullable for GLOBAL/REGION
    String regionCode,   // nullable; set when scope = REGION
    String region,       // nullable sub-national region name
    String startDate,    // ISO date
    String endDate,      // ISO date
    String recurrence,   // NONE | YEARLY
    boolean isSystem
) {}
