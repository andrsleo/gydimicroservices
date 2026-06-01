package com.affiliate.rentals.gydi.collaborations.domain.event;

/**
 * Published when a creator uploads a delivery asset for a deliverable.
 *
 * @param agreementId          the agreement
 * @param deliverableId        the specific deliverable
 * @param assetId              the uploaded asset
 * @param hostId               host to notify for review
 * @param creatorId            uploading creator
 */
public record DeliverySubmittedEvent(Long agreementId, Long deliverableId, Long assetId,
                                      Long hostId, Long creatorId) {}
