package com.affiliate.rentals.gydi.properties.domain.ports.in;

import com.affiliate.rentals.gydi.properties.domain.model.Property;

/**
 * Use case for importing property data from an Airbnb listing URL.
 *
 * This use case:
 * 1. Validates the Airbnb URL
 * 2. Extracts metadata from the listing
 * 3. Pre-fills property data with extracted information
 * 4. Allows user to complete missing fields
 * 5. Saves property with SEMI_IMPORT mode
 */
public interface ImportPropertyFromAirbnbUseCase {

    /**
     * Validates an Airbnb URL and extracts basic metadata for preview.
     * This is a lightweight operation for real-time validation in the UI.
     *
     * @param command the validation command with URL
     * @return validation result with basic metadata
     */
    ValidateAirbnbUrlResult validateAirbnbUrl(ValidateAirbnbUrlCommand command);

    /**
     * Imports a property from Airbnb by extracting metadata and creating a draft.
     * The property is created in DRAFT status with import_mode = SEMI_IMPORT.
     *
     * @param command the import command with URL and additional property data
     * @return the created Property with imported data
     */
    Property importPropertyFromAirbnb(ImportPropertyFromAirbnbCommand command);

    /**
     * Command for validating an Airbnb URL
     */
    record ValidateAirbnbUrlCommand(
            String airbnbUrl) {
    }

    /**
     * Result of Airbnb URL validation
     */
    record ValidateAirbnbUrlResult(
            boolean valid,
            String listingId,
            String title,
            String description,
            String imageUrl,
            Integer bedrooms,
            Double bathrooms,
            Integer maxGuests,
            java.util.List<String> amenities,
            String city,
            String country,
            String address,
            String errorMessage) {
        public static ValidateAirbnbUrlResult valid(
                String listingId,
                String title,
                String description,
                String imageUrl,
                Integer bedrooms,
                Double bathrooms,
                Integer maxGuests,
                java.util.List<String> amenities,
                String city,
                String country,
                String address) {
            return new ValidateAirbnbUrlResult(
                    true, listingId, title, description, imageUrl,
                    bedrooms, bathrooms, maxGuests, amenities, city, country, address,
                    null);
        }

        public static ValidateAirbnbUrlResult invalid(String errorMessage) {
            return new ValidateAirbnbUrlResult(
                    false, null, null, null, null,
                    null, null, null, null, null, null, null,
                    errorMessage);
        }
    }

    /**
     * Command for importing property from Airbnb.
     * Contains the Airbnb URL plus additional required fields that
     * cannot be automatically extracted.
     */
    record ImportPropertyFromAirbnbCommand(
            Long hostId,
            String airbnbUrl,
            // Required fields that user must provide
            java.math.BigDecimal priceAmount,
            String priceCurrency,
            String country,
            String city,
            String address,
            String postalCode,
            int bedrooms,
            int bathrooms,
            int maxGuests,
            String propertyType,
            String listingType,
            // Optional: User can override imported title/description
            String titleOverride,
            String descriptionOverride,
            // Optional: Additional amenities not extracted
            java.util.List<String> additionalAmenities) {
    }
}
