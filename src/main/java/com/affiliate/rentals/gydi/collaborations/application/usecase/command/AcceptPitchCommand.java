package com.affiliate.rentals.gydi.collaborations.application.usecase.command;

/**
 * Command to accept a pitch or the current counter-offer state.
 *
 * @author GYDI Development Team
 */
public record AcceptPitchCommand(
        Long pitchId,
        Long requestingUserId
) {}
