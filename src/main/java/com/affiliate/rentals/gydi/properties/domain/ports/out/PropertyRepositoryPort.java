package com.affiliate.rentals.gydi.properties.domain.ports.out;

import com.affiliate.rentals.gydi.properties.domain.model.Property;
import com.affiliate.rentals.gydi.properties.domain.model.PropertyId;
import com.affiliate.rentals.gydi.properties.domain.model.PropertyStatus;
import com.affiliate.rentals.gydi.properties.domain.model.PropertyType;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Repository port for Property persistence operations.
 */
public interface PropertyRepositoryPort {

    /**
     * Saves a property (create or update).
     *
     * @param property the property to save
     * @return the saved property with generated ID if new
     */
    Property save(Property property);

    /**
     * Finds a property by ID.
     *
     * @param id the property ID
     * @return the property if found
     */
    Optional<Property> findById(PropertyId id);

    /**
     * Finds a property by its SEO-friendly slug.
     *
     * @param slug the property slug (e.g., "beach-house-malibu-x7k2m")
     * @return the property if found
     */
    Optional<Property> findBySlug(String slug);

    /**
     * Finds all properties by host ID.
     *
     * @param hostId the host user ID
     * @return list of properties owned by the host
     */
    List<Property> findByHostId(Long hostId);

    /**
     * Finds all properties that have an Airbnb iCal URL configured.
     *
     * @return list of properties with iCal URLs
     */
    List<Property> findAllWithIcalUrl();

    /**
     * Finds properties with filters and pagination.
     *
     * @param spec the search specification
     * @return paginated list of properties
     */
    PropertySearchResult findAll(PropertySearchSpec spec);

    /**
     * Deletes a property by ID.
     *
     * @param id the property ID
     */
    void deleteById(PropertyId id);

    /**
     * Checks if a property exists by ID.
     *
     * @param id the property ID
     * @return true if exists
     */
    boolean existsById(PropertyId id);

    /**
     * Checks if a property exists with the given Airbnb listing ID.
     * Used to prevent duplicate imports of the same Airbnb listing.
     *
     * @param airbnbListingId the Airbnb listing ID
     * @return true if a property with this listing ID exists
     */
    boolean existsByAirbnbListingId(String airbnbListingId);

    /**
     * Search specification for querying properties
     */
    record PropertySearchSpec(
            PropertyStatus status,
            PropertyType propertyType,
            String listingType,
            String country,
            String city,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Integer minBedrooms,
            Integer minBathrooms,
            Integer minGuests,
            List<String> amenities,
            String searchText,
            int page,
            int size,
            String sortBy,
            String sortDirection) {
    }

    /**
     * Search result with pagination
     */
    record PropertySearchResult(
            List<Property> properties,
            long totalElements,
            int totalPages,
            int currentPage,
            int pageSize) {
    }
}
