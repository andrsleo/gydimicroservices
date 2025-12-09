package com.affiliate.rentals.gydi.properties.domain.model;

/**
 * Enum representing how property data was created or imported.
 */
public enum ImportMode {
    /**
     * Property data was entered manually by the user
     */
    MANUAL,

    /**
     * Property data was semi-automatically imported from Airbnb
     * (user provided URL, system extracted metadata)
     */
    SEMI_IMPORT
}
