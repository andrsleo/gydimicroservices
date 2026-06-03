package com.affiliate.rentals.gydi.content.domain.event;

/**
 * Domain event published when a new content post is created.
 *
 * @param contentPostId the ID of the newly created content post
 * @param creatorId     the ID of the creator
 * @param propertyId    the optional linked property ID (may be null)
 * @author GYDI Development Team
 */
public record ContentCreatedEvent(Long contentPostId, Long creatorId, Long propertyId) {
}
