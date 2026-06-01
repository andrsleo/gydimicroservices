package com.affiliate.rentals.gydi.collaborations.domain.event;

/**
 * Published when a host requests a revision on a deliverable.
 *
 * @param agreementId   the agreement
 * @param deliverableId the deliverable needing revision
 * @param creatorId     creator to notify
 * @param feedback      revision instructions from the host
 */
public record RevisionRequestedEvent(Long agreementId, Long deliverableId,
                                      Long creatorId, String feedback) {}
