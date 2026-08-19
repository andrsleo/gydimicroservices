package com.affiliate.rentals.gydi.properties.domain.ports.in;

import com.affiliate.rentals.gydi.properties.domain.model.Property;
import com.affiliate.rentals.gydi.properties.domain.model.PropertyStatus;
import com.affiliate.rentals.gydi.properties.domain.model.PropertyType;

import java.math.BigDecimal;
import java.util.List;

/**
 * Use case for listing properties with filters and pagination.
 */
public interface ListPropertiesUseCase {
    
    /**
     * Lists properties with filters.
     *
     * @param query the search query with filters and pagination
     * @return paginated result with properties
     */
    PropertyPage listProperties(ListPropertiesQuery query);
    
    /**
     * Query for listing properties
     */
    record ListPropertiesQuery(
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
        String sortDirection
    ) {
        public ListPropertiesQuery {
            if (page < 0) page = 0;
            if (size <= 0 || size > 100) size = 20;
        }
    }
    
    /**
     * Paginated result
     */
    record PropertyPage(
        List<Property> properties,
        long totalElements,
        int totalPages,
        int currentPage,
        int pageSize
    ) {}
}
