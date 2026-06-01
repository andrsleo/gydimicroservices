package com.affiliate.rentals.gydi.collaborations.domain.event;

/**
 * Published when a pitch (or counter-offer) is accepted.
 * Triggers agreement generation.
 *
 * @param pitchId     the accepted pitch
 * @param agreementId the generated agreement
 * @param hostId      host party
 * @param creatorId   creator party
 */
public record PitchAcceptedEvent(Long pitchId, Long agreementId, Long hostId, Long creatorId) {}
