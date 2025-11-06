package com.affiliate.rentals.gydi.properties.domain.ports.in;

import com.affiliate.rentals.gydi.properties.domain.model.Property;
import com.affiliate.rentals.gydi.properties.domain.model.PropertyId;

/**
 * Use case for creating a new property in DRAFT status.
 */
public interface CreatePropertyUseCase {
    
    /**
     * Creates a new property.
     *
     * @param command the creation command with property data
     * @return the created Property with generated ID
     */
    Property createProperty(CreatePropertyCommand command);
    
    /**
     * Command for creating a property
     */
    record CreatePropertyCommand(
        Long hostId,
        String title,
        String description,
        java.math.BigDecimal priceAmount,
        String priceCurrency,
        String country,
        String city,
        String address,
        String postalCode,
        java.util.List<String> amenities,
        int bedrooms,
        int bathrooms,
        int maxGuests,
        String propertyType
    ) {}
}
