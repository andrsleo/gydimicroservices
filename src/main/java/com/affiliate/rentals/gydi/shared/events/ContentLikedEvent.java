package com.affiliate.rentals.gydi.shared.events;

/**
 * Domain event published when a user likes a content post.
 * Consumed by the notifications bounded context.
 */
public record ContentLikedEvent(
    Long contentPostId,
    Long creatorUserId,   // owner of the post — receives notification
    Long likedByUserId    // who pressed like
) {}
