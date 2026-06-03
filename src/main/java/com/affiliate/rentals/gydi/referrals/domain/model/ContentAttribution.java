package com.affiliate.rentals.gydi.referrals.domain.model;

import java.time.LocalDateTime;

/**
 * Domain model for content attribution.
 * Links a booking to a content post and creator for commission attribution.
 * Pure Java — no framework dependencies.
 */
public class ContentAttribution {

    private Long id;
    private Long bookingId;
    private Long contentPostId;
    private Long creatorId;
    private Long referralLinkId;
    private AttributionType attributionType;
    private LocalDateTime createdAt;

    private ContentAttribution() {}

    /**
     * Factory method to create a new ContentAttribution.
     */
    public static ContentAttribution create(
            Long bookingId,
            Long contentPostId,
            Long creatorId,
            Long referralLinkId,
            AttributionType attributionType) {

        if (creatorId == null || creatorId <= 0) {
            throw new IllegalArgumentException("creatorId must be a positive value");
        }
        if (contentPostId == null || contentPostId <= 0) {
            throw new IllegalArgumentException("contentPostId must be a positive value");
        }
        if (attributionType == null) {
            throw new IllegalArgumentException("attributionType cannot be null");
        }

        ContentAttribution attribution = new ContentAttribution();
        attribution.bookingId = bookingId;
        attribution.contentPostId = contentPostId;
        attribution.creatorId = creatorId;
        attribution.referralLinkId = referralLinkId;
        attribution.attributionType = attributionType;
        attribution.createdAt = LocalDateTime.now();
        return attribution;
    }

    /**
     * Factory method to reconstitute from persistence.
     */
    public static ContentAttribution reconstitute(
            Long id,
            Long bookingId,
            Long contentPostId,
            Long creatorId,
            Long referralLinkId,
            AttributionType attributionType,
            LocalDateTime createdAt) {

        ContentAttribution attribution = new ContentAttribution();
        attribution.id = id;
        attribution.bookingId = bookingId;
        attribution.contentPostId = contentPostId;
        attribution.creatorId = creatorId;
        attribution.referralLinkId = referralLinkId;
        attribution.attributionType = attributionType;
        attribution.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
        return attribution;
    }

    // Getters
    public Long getId() { return id; }
    public Long getBookingId() { return bookingId; }
    public Long getContentPostId() { return contentPostId; }
    public Long getCreatorId() { return creatorId; }
    public Long getReferralLinkId() { return referralLinkId; }
    public AttributionType getAttributionType() { return attributionType; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    // Setter for id — set by persistence after save
    public void setId(Long id) { this.id = id; }
}
