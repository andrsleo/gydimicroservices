package com.affiliate.rentals.gydi.shared.events;

/**
 * Domain event published when a content post reaches a view milestone (1K, 10K, 100K).
 * Consumed by the notifications bounded context.
 */
public record ContentMilestoneEvent(
    Long contentPostId,
    Long creatorUserId,   // receives notification
    long milestoneViews   // e.g. 1000, 10000, 100000
) {}
