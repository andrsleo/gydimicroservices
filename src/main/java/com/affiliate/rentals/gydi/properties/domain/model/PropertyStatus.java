package com.affiliate.rentals.gydi.properties.domain.model;

/**
 * Property lifecycle status
 */
public enum PropertyStatus {
    /**
     * Property is being created, not visible to users
     */
    DRAFT,

    /**
     * Property is published and visible in catalog
     */
    PUBLISHED,

    /**
     * Property is temporarily inactive (not visible to users)
     */
    INACTIVE,

    /**
     * Property is soft-deleted
     */
    DELETED
}
