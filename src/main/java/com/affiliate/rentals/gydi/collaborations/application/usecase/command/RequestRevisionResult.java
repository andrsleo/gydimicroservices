package com.affiliate.rentals.gydi.collaborations.application.usecase.command;

/**
 * Result of requesting a revision on a deliverable.
 */
public record RequestRevisionResult(
        Long deliverableId,
        String deliverableStatus,
        String feedback
) {}
