package com.affiliate.rentals.gydi.calendar.application.usecase;

import com.affiliate.rentals.gydi.calendar.application.dto.CreateSeasonDefinitionRequest;
import com.affiliate.rentals.gydi.calendar.application.dto.SeasonDefinitionDto;
import com.affiliate.rentals.gydi.calendar.domain.model.SeasonDefinition;
import com.affiliate.rentals.gydi.calendar.domain.model.SeasonType;
import com.affiliate.rentals.gydi.calendar.domain.port.in.CreateSeasonDefinitionUseCase;
import com.affiliate.rentals.gydi.calendar.domain.port.out.SeasonDefinitionRepositoryPort;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class CreateSeasonDefinitionUseCaseImpl implements CreateSeasonDefinitionUseCase {

    private final SeasonDefinitionRepositoryPort repository;

    public CreateSeasonDefinitionUseCaseImpl(SeasonDefinitionRepositoryPort repository) {
        this.repository = repository;
    }

    @Override
    public SeasonDefinitionDto execute(CreateSeasonDefinitionRequest request) {
        SeasonDefinition season = SeasonDefinition.create(
                request.name(),
                SeasonType.valueOf(request.seasonType()),
                request.scope(),
                request.country(),
                request.regionCode(),
                request.region(),
                LocalDate.parse(request.startDate()),
                LocalDate.parse(request.endDate()),
                request.recurrence()
        );

        SeasonDefinition saved = repository.save(season);
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
