package com.affiliate.rentals.gydi.collaborations.domain.event;

/**
 * Published when the expiration scheduler marks a pitch as expired.
 *
 * @param pitchId   the expired pitch
 * @param hostId    host party
 * @param creatorId creator party
 */
public record PitchExpiredEvent(Long pitchId, Long hostId, Long creatorId) {}
