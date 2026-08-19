package com.affiliate.rentals.gydi.properties.application.dto;

/**
 * DTO for property specifications.
 */
public record PropertySpecsDTO(
    int bedrooms,
    int bathrooms,
    int maxGuests
) {}
