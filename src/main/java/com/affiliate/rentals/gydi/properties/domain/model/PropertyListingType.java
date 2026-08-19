package com.affiliate.rentals.gydi.properties.domain.model;

/**
 * Defines the listing type for a property.
 * Determines how the property is marketed and what workflows apply.
 */
public enum PropertyListingType {
    /**
     * Property available for short-term vacation rentals only
     */
    SHORT_TERM_RENTAL("Renta Corta", "Short-term rental property"),

    /**
     * Property available for sale only
     */
    SALE("Venta", "Property for sale"),

    /**
     * Property available for both rental and sale
     */
    BOTH("Ambas", "Property for rental or sale");

    private final String displayNameEs;
    private final String description;

    PropertyListingType(String displayNameEs, String description) {
        this.displayNameEs = displayNameEs;
        this.description = description;
    }

    public String getDisplayNameEs() {
        return displayNameEs;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Checks if this listing type includes rental capability
     */
    public boolean allowsRental() {
        return this == SHORT_TERM_RENTAL || this == BOTH;
    }

    /**
     * Checks if this listing type includes sale capability
     */
    public boolean allowsSale() {
        return this == SALE || this == BOTH;
    }

    /**
     * Validates if a listing type transition is allowed.
     * All transitions are currently allowed to support property flexibility.
     */
    public boolean canTransitionTo(PropertyListingType newType) {
        // Business rule: All transitions are allowed for MVP
        // This method exists for future business rules
        return newType != null;
    }
}
