package com.affiliate.rentals.gydi.properties.application.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO for creating a new property.
 */
public record CreatePropertyRequest(
    
    @NotBlank(message = "Title is required")
    @Size(min = 10, max = 100, message = "Title must be between 10 and 100 characters")
    String title,
    
    @Size(max = 2000, message = "Description cannot exceed 2000 characters")
    String description,
    
    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.01", message = "Price must be greater than zero")
    BigDecimal pricePerNight,
    
    @NotBlank(message = "Currency is required")
    @Pattern(regexp = "USD|EUR|MXN|CAD|GBP", message = "Invalid currency")
    String currency,
    
    @NotBlank(message = "Country is required")
    String country,
    
    @NotBlank(message = "City is required")
    String city,
    
    String address,
    
    String postalCode,
    
    List<String> amenities,
    
    @NotNull(message = "Bedrooms is required")
    @Min(value = 0, message = "Bedrooms cannot be negative")
    Integer bedrooms,
    
    @NotNull(message = "Bathrooms is required")
    @Min(value = 0, message = "Bathrooms cannot be negative")
    Integer bathrooms,
    
    @NotNull(message = "Max guests is required")
    @Min(value = 1, message = "Max guests must be at least 1")
    Integer maxGuests,
    
    @NotBlank(message = "Property type is required")
    @Pattern(regexp = "APARTMENT|HOUSE|VILLA|CABIN|STUDIO|CONDO|BUNGALOW|OTHER", 
             message = "Invalid property type")
    String propertyType
) {}
