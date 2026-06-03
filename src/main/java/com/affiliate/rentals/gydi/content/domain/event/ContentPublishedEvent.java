package com.affiliate.rentals.gydi.content.domain.event;

/**
 * Domain event published when a content post is published.
 *
 * @param contentPostId the ID of the published content post
 * @param creatorId     the ID of the creator
 * @author GYDI Development Team
 */
public record ContentPublishedEvent(Long contentPostId, Long creatorId) {
}
