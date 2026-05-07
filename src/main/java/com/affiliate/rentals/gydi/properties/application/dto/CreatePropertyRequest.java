package com.affiliate.rentals.gydi.properties.application.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO for creating a new property.
 */
public record CreatePropertyRequest(

        @NotBlank(message = "Title is required") @Size(min = 10, max = 100, message = "Title must be between 10 and 100 characters") String title,

        @NotBlank(message = "Description is required") @Size(min = 20, max = 2000, message = "Description must be between 20 and 2000 characters") String description,

        @NotNull(message = "Price is required") @DecimalMin(value = "0.01", message = "Price must be greater than zero") BigDecimal pricePerNight,

        @NotBlank(message = "Currency is required") @Size(min = 3, max = 10, message = "Currency code must be 3-10 characters") String currency,

        @DecimalMin(value = "0.01", message = "Sale price must be greater than zero") BigDecimal salePrice,

        @NotBlank(message = "Country is required") String country,

        @NotBlank(message = "City is required") String city,

        @NotBlank(message = "Address is required") @Size(min = 5, max = 200, message = "Address must be between 5 and 200 characters") String address,

        @NotBlank(message = "Postal code is required") @Size(min = 3, max = 20, message = "Postal code must be between 3 and 20 characters") String postalCode,

        @NotNull(message = "Amenities are required") @Size(min = 3, max = 50, message = "Please select between 3 and 50 amenities") List<String> amenities,

        @NotNull(message = "Bedrooms is required") @Min(value = 0, message = "Bedrooms cannot be negative") Integer bedrooms,

        @NotNull(message = "Bathrooms is required") @Min(value = 0, message = "Bathrooms cannot be negative") Integer bathrooms,

        @NotNull(message = "Max guests is required") @Min(value = 1, message = "Max guests must be at least 1") Integer maxGuests,

        @NotBlank(message = "Property type is required") @Pattern(regexp = "APARTMENT|HOUSE|VILLA|CABIN|STUDIO|CONDO|BUNGALOW|OTHER", message = "Invalid property type") String propertyType,

        @Pattern(regexp = "SHORT_TERM_RENTAL|SALE|BOTH", message = "Invalid listing type. Must be SHORT_TERM_RENTAL, SALE, or BOTH") String listingType,

        @NotBlank(message = "Airbnb URL is required") String airbnbUrl,

        String icalUrlAirbnb) {
}
