package com.affiliate.rentals.gydi.collaborations.application.usecase.command;

import com.affiliate.rentals.gydi.collaborations.domain.model.enums.PitchStatus;

/**
 * Result returned after accepting a pitch.
 *
 * @author GYDI Development Team
 */
public record AcceptPitchResult(
        Long pitchId,
        PitchStatus status,
        Long agreementId
) {}
