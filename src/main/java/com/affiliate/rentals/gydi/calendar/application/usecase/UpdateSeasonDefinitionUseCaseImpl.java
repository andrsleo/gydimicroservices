package com.affiliate.rentals.gydi.calendar.application.usecase;

import com.affiliate.rentals.gydi.calendar.application.dto.SeasonDefinitionDto;
import com.affiliate.rentals.gydi.calendar.application.dto.UpdateSeasonDefinitionRequest;
import com.affiliate.rentals.gydi.calendar.domain.model.SeasonDefinition;
import com.affiliate.rentals.gydi.calendar.domain.model.SeasonType;
import com.affiliate.rentals.gydi.calendar.domain.port.in.UpdateSeasonDefinitionUseCase;
import com.affiliate.rentals.gydi.calendar.domain.port.out.SeasonDefinitionRepositoryPort;
import com.affiliate.rentals.gydi.calendar.domain.exception.SeasonDefinitionNotFoundException;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

@Service
public class UpdateSeasonDefinitionUseCaseImpl implements UpdateSeasonDefinitionUseCase {

    private final SeasonDefinitionRepositoryPort repository;

    public UpdateSeasonDefinitionUseCaseImpl(SeasonDefinitionRepositoryPort repository) {
        this.repository = repository;
    }

    @Override
    public SeasonDefinitionDto execute(Long id, UpdateSeasonDefinitionRequest request) {
        SeasonDefinition existing = repository.findById(id)
                .orElseThrow(() -> new SeasonDefinitionNotFoundException(id));

        // Reconstruct with updated fields, preserving id/isSystem/createdAt
        SeasonDefinition updated = SeasonDefinition.reconstruct(
                existing.getId(),
                request.name(),
                SeasonType.valueOf(request.seasonType()),
                request.scope(),
                request.country(),
                request.regionCode(),
                request.region(),
                LocalDate.parse(request.startDate()),
                LocalDate.parse(request.endDate()),
                request.recurrence(),
                existing.isSystem(),
                existing.getCreatedAt(),
                ZonedDateTime.now(ZoneOffset.UTC)
        );

        SeasonDefinition saved = repository.save(updated);
        return toDto(saved);
    }

    private SeasonDefinitionDto toDto(SeasonDefinition s) {
        return new SeasonDefinitionDto(
                s.getId(), s.getName(), s.getSeasonType().name(),
                s.getScope(), s.getCountry(), s.getRegionCode(), s.getRegion(),
                s.getStartDate().toString(), s.getEndDate().toString(),
                s.getRecurrence(), s.isSystem());
    }
}
