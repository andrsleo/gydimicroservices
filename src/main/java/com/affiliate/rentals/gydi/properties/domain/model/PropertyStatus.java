package com.affiliate.rentals.gydi.properties.domain.model;

/**
 * Property lifecycle status with admin approval flow.
 *
 * State machine:
 *   DRAFT → PENDING_APPROVAL  (auto, when minimums met on save or image upload/delete)
 *   PENDING_APPROVAL → PUBLISHED  (ADMIN approves)
 *   PENDING_APPROVAL → DENY       (ADMIN rejects)
 *   DENY → PENDING_APPROVAL       (host re-submits after edits)
 *   PUBLISHED → INACTIVE          (host deactivates or ADMIN)
 *   INACTIVE → PUBLISHED          (host reactivates or ADMIN)
 */
public enum PropertyStatus {
    /**
     * Property is being created/edited by host. Not visible to public.
     * Automatically transitions to PENDING_APPROVAL when all required fields are
     * present and imageCount >= 4.
     */
    DRAFT,

    /**
     * Property submitted for admin review. Not visible to public.
     * Reached automatically from DRAFT when minimums are met on save or image change.
     * Also reached from DENY when the host re-submits after corrections.
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
