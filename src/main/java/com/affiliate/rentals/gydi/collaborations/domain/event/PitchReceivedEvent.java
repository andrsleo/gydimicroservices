package com.affiliate.rentals.gydi.collaborations.domain.event;

/**
 * Published when a creator submits a new pitch to a host.
 *
 * @param pitchId    ID of the new pitch
 * @param propertyId target property
 * @param hostId     receiving host
 * @param creatorId  submitting creator
 */
public record PitchReceivedEvent(Long pitchId, Long propertyId, Long hostId, Long creatorId) {}
