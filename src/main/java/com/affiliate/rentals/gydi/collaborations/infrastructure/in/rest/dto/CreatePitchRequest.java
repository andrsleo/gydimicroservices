package com.affiliate.rentals.gydi.collaborations.infrastructure.in.rest.dto;

import com.affiliate.rentals.gydi.collaborations.domain.model.PitchCompensation;
import com.affiliate.rentals.gydi.collaborations.domain.model.PitchDeliverable;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

public record CreatePitchRequest(
        @NotNull Long propertyId,
        @NotNull @Size(min = 10, max = 1000) String introduction,
        String portfolioUrl,
        @NotNull LocalDate preferredCheckIn,
        @NotNull LocalDate preferredCheckOut,
        @NotNull @Valid List<PitchDeliverable> deliverables,
        @NotNull @Valid PitchCompensation compensation
) {}
