package com.affiliate.rentals.gydi.properties.application.dto;

/**
 * DTO for property location.
 */
public record PropertyLocationDTO(
    String country,
    String city,
    String address,
    String postalCode
) {}
