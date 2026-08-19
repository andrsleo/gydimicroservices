package com.affiliate.rentals.gydi.collaborations.domain.event;

/**
 * Published when an agreement is cancelled by host or creator.
 *
 * @param agreementId the cancelled agreement
 * @param pitchId     the associated pitch
 * @param hostId      host party
 * @param creatorId   creator party
 * @param cancelledBy HOST or CREATOR
 */
public record AgreementCancelledEvent(Long agreementId, Long pitchId,
                                       Long hostId, Long creatorId, String cancelledBy) {}
