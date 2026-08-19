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
     * Command for creating a property.
     *
     * <p><strong>Currency invariant:</strong> {@code priceCurrency} is the single currency
     * for the entire property — it applies to BOTH {@code priceAmount} (pricePerNight)
     * and {@code salePrice}. A property may never have mixed currencies across its prices.
     * If independent sale-price currency is ever needed, add a separate field and validate
     * equality in {@link com.affiliate.rentals.gydi.properties.application.usecase.CreatePropertyUseCaseImpl}.
     */
    record CreatePropertyCommand(
            Long hostId,
            String title,
            String description,
            java.math.BigDecimal priceAmount,
            String priceCurrency,
            java.math.BigDecimal salePrice,
            String country,
            String city,
            String address,
            String postalCode,
            java.util.List<String> amenities,
            int bedrooms,
            int bathrooms,
            int maxGuests,
            String propertyType,
            String listingType,
            String airbnbUrl,
            String icalUrlAirbnb) {
    }
}
