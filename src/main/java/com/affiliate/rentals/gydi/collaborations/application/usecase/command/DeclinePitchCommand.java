package com.affiliate.rentals.gydi.collaborations.application.usecase.command;

/**
 * Command to decline a pitch or the current counter-offer state.
 *
 * @author GYDI Development Team
 */
public record DeclinePitchCommand(
        Long pitchId,
        Long requestingUserId,
        String reason
) {}
