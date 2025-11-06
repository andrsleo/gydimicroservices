package com.affiliate.rentals.gydi.properties.domain.ports.in;

import com.affiliate.rentals.gydi.properties.domain.model.Property;
import com.affiliate.rentals.gydi.properties.domain.model.PropertyId;

import java.util.Optional;

/**
 * Use case for retrieving a property by its ID.
 */
public interface GetPropertyByIdUseCase {
    
    /**
     * Gets a property by ID.
     *
     * @param query the query with property ID
     * @return the Property if found
     */
    Optional<Property> getProperty(GetPropertyQuery query);
    
    /**
     * Query for getting a property by ID
     */
    record GetPropertyQuery(
        PropertyId propertyId,
        boolean includeInactive
    ) {
        public GetPropertyQuery(PropertyId propertyId) {
            this(propertyId, false);
        }
    }
}
