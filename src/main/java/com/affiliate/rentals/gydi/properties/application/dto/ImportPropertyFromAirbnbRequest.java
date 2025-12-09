package com.affiliate.rentals.gydi.properties.application.dto;

import java.math.BigDecimal;
import java.util.List;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Request DTO for importing a property from Airbnb.
 * Contains the Airbnb URL plus required fields that cannot be automatically extracted.
 */
public record ImportPropertyFromAirbnbRequest(
    @NotBlank(message = "Airbnb URL is required")
    String airbnbUrl,

    // Required fields that user must provide
    @NotNull(message = "Price per night is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Price must be greater than 0")
    BigDecimal priceAmount,

    @NotBlank(message = "Currency is required")
    String priceCurrency,

    @NotBlank(message = "Country is required")
    String country,

    @NotBlank(message = "City is required")
    String city,

    @NotBlank(message = "Address is required")
    String address,

    String postalCode,

    @Min(value = 0, message = "Bedrooms must be non-negative")
    int bedrooms,

    @Min(value = 0, message = "Bathrooms must be non-negative")
    int bathrooms,

    @Min(value = 1, message = "Max guests must be at least 1")
    int maxGuests,

    @NotBlank(message = "Property type is required")
    String propertyType,

    @NotBlank(message = "Listing type is required")
    String listingType,

    // Optional: User can override imported title/description
    String titleOverride,
    String descriptionOverride,

    // Optional: Additional amenities not extracted
    List<String> additionalAmenities
) {}
