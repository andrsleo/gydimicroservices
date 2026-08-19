package com.affiliate.rentals.gydi.collaborations.domain.event;

/**
 * Published when a host approves a deliverable.
 *
 * @param agreementId   the agreement
 * @param deliverableId the approved deliverable
 * @param creatorId     creator to notify
 * @param allApproved   true if all deliverables are now approved (agreement will complete)
 */
public record DeliveryApprovedEvent(Long agreementId, Long deliverableId,
                                     Long creatorId, boolean allApproved) {}
