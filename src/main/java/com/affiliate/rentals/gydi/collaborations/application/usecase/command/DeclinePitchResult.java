package com.affiliate.rentals.gydi.collaborations.application.usecase.command;

import com.affiliate.rentals.gydi.collaborations.domain.model.enums.PitchStatus;

/**
 * Result returned after declining a pitch.
 *
 * @author GYDI Development Team
 */
public record DeclinePitchResult(
        Long pitchId,
        PitchStatus status
) {}
