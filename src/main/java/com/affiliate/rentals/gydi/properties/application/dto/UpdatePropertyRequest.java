package com.affiliate.rentals.gydi.properties.application.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO for updating an existing property.
 */
public record UpdatePropertyRequest(
    
    @Size(min = 10, max = 100, message = "Title must be between 10 and 100 characters")
    String title,
    
    @Size(max = 2000, message = "Description cannot exceed 2000 characters")
    String description,
    
    @DecimalMin(value = "0.01", message = "Price must be greater than zero")
    BigDecimal pricePerNight,

    @Pattern(regexp = "USD|EUR|MXN|COP|CAD|GBP", message = "Invalid currency")
    String currency,

    @DecimalMin(value = "0.01", message = "Sale price must be greater than zero")
    BigDecimal salePrice,
    
    String country,
    
    String city,
    
    String address,
    
    String postalCode,
    
    List<String> amenities,
    
    @Min(value = 0, message = "Bedrooms cannot be negative")
    Integer bedrooms,
    
    @Min(value = 0, message = "Bathrooms cannot be negative")
    Integer bathrooms,
    
    @Min(value = 1, message = "Max guests must be at least 1")
    Integer maxGuests,

    @Pattern(regexp = "SHORT_TERM_RENTAL|SALE|BOTH",
             message = "Invalid listing type. Must be SHORT_TERM_RENTAL, SALE, or BOTH")
    String listingType
) {}
