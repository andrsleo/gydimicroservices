package com.affiliate.rentals.gydi.content.domain.event;

/**
 * Domain event published when a content post is archived.
 *
 * @param contentPostId the ID of the archived content post
 * @param creatorId     the ID of the creator
 * @author GYDI Development Team
 */
public record ContentArchivedEvent(Long contentPostId, Long creatorId) {
}
