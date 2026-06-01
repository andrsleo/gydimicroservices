package com.affiliate.rentals.gydi.collaborations.domain.event;

/**
 * Published when a pitch or counter-offer is declined.
 *
 * @param pitchId   the declined pitch
 * @param hostId    host party
 * @param creatorId creator party
 * @param reason    optional decline reason
 */
public record PitchDeclinedEvent(Long pitchId, Long hostId, Long creatorId, String reason) {}
