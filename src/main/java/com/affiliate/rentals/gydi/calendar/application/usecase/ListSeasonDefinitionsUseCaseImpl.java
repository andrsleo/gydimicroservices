package com.affiliate.rentals.gydi.calendar.application.usecase;

import com.affiliate.rentals.gydi.calendar.application.dto.SeasonDefinitionDto;
import com.affiliate.rentals.gydi.calendar.domain.model.SeasonDefinition;
import com.affiliate.rentals.gydi.calendar.domain.port.in.ListSeasonDefinitionsUseCase;
import com.affiliate.rentals.gydi.calendar.domain.port.out.SeasonDefinitionRepositoryPort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ListSeasonDefinitionsUseCaseImpl implements ListSeasonDefinitionsUseCase {

    private final SeasonDefinitionRepositoryPort repository;

    public ListSeasonDefinitionsUseCaseImpl(SeasonDefinitionRepositoryPort repository) {
        this.repository = repository;
    }

    @Override
    public List<SeasonDefinitionDto> execute() {
        return repository.findAll().stream()
                .map(this::toDto)
                .toList();
    }

    private SeasonDefinitionDto toDto(SeasonDefinition s) {
        return new SeasonDefinitionDto(
                s.getId(), s.getName(), s.getSeasonType().name(),
                s.getScope(), s.getCountry(), s.getRegionCode(), s.getRegion(),
                s.getStartDate().toString(), s.getEndDate().toString(),
                s.getRecurrence(), s.isSystem());
    }
}
