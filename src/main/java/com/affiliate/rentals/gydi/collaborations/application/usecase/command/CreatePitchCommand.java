package com.affiliate.rentals.gydi.collaborations.application.usecase.command;

import com.affiliate.rentals.gydi.collaborations.domain.model.PitchCompensation;
import com.affiliate.rentals.gydi.collaborations.domain.model.PitchDeliverable;

import java.time.LocalDate;
import java.util.List;

/**
 * Command to submit a new creator pitch for a property.
 *
 * @author GYDI Development Team
 */
public record CreatePitchCommand(
        Long propertyId,
        Long creatorId,
        String introduction,
        String portfolioUrl,
        LocalDate preferredCheckIn,
        LocalDate preferredCheckOut,
        List<PitchDeliverable> deliverables,
        PitchCompensation compensation
) {}
