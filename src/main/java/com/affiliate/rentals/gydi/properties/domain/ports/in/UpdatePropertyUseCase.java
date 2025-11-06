package com.affiliate.rentals.gydi.properties.domain.ports.in;

import com.affiliate.rentals.gydi.properties.domain.model.Property;
import com.affiliate.rentals.gydi.properties.domain.model.PropertyId;

/**
 * Use case for updating an existing property.
 */
public interface UpdatePropertyUseCase {
    
    /**
     * Updates property details.
     *
     * @param command the update command
     * @return the updated Property
     */
    Property updateProperty(UpdatePropertyCommand command);
    
    /**
     * Command for updating a property
     */
    record UpdatePropertyCommand(
        PropertyId propertyId,
        Long requestingUserId,
        String title,
        String description,
        java.math.BigDecimal priceAmount,
        String priceCurrency,
        String country,
        String city,
        String address,
        String postalCode,
        java.util.List<String> amenities,
        Integer bedrooms,
        Integer bathrooms,
        Integer maxGuests
    ) {}
}
