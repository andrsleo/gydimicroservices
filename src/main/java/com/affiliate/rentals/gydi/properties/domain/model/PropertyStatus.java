package com.affiliate.rentals.gydi.properties.domain.model;

/**
 * Property lifecycle status with admin approval flow.
 */
public enum PropertyStatus {
    /**
     * Property is being created/edited by host. Not visible to public.
     */
    DRAFT,

    /**
     * Minimum content requirements are met. Host must add gydiproperties@gmail.com as a
     * co-host on Airbnb before the property can be submitted for admin review.
     * Reached automatically from DRAFT when an update satisfies all content validations.
     * Also reached from PUBLISHED/INACTIVE when the iCal/Airbnb URL changes.
     */
    SEND_GYDI_COHOST,

    /**
     * Host submitted property for admin review. Not visible to public.
     */
    PENDING_APPROVAL,

    /**
     * Admin approved the property. Visible to public in catalog.
     */
    PUBLISHED,

    /**
     * Property is temporarily disabled by host or admin. Not visible to public.
     */
    INACTIVE,

    /**
     * Admin rejected the property. Host can fix and re-submit.
     */
    DENY,

    /**
     * Property is soft-deleted.
     */
    DELETED
}
