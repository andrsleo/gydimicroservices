package com.affiliate.rentals.gydi.calendar.application.usecase;

import com.affiliate.rentals.gydi.calendar.application.dto.SeasonDefinitionDto;
import com.affiliate.rentals.gydi.calendar.domain.model.SeasonDefinition;
import com.affiliate.rentals.gydi.calendar.domain.port.in.GetSeasonsByCountryUseCase;
import com.affiliate.rentals.gydi.calendar.domain.port.out.SeasonDefinitionRepositoryPort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GetSeasonsByCountryUseCaseImpl implements GetSeasonsByCountryUseCase {

    private final SeasonDefinitionRepositoryPort seasonDefinitionRepository;

    public GetSeasonsByCountryUseCaseImpl(SeasonDefinitionRepositoryPort seasonDefinitionRepository) {
        this.seasonDefinitionRepository = seasonDefinitionRepository;
    }

    @Override
    public List<SeasonDefinitionDto> execute(String country) {
        // findByCountry now resolves the full scope hierarchy:
        // GLOBAL + matching REGION + COUNTRY + SUBREGION
        List<SeasonDefinition> seasons = seasonDefinitionRepository.findByCountry(country);

        return seasons.stream()
                .map(this::toDto)
                .toList();
    }

    private SeasonDefinitionDto toDto(SeasonDefinition s) {
        return new SeasonDefinitionDto(
                s.getId(),
                s.getName(),
                s.getSeasonType().name(),
                s.getScope(),
                s.getCountry(),
                s.getRegionCode(),
                s.getRegion(),
                s.getStartDate().toString(),
                s.getEndDate().toString(),
                s.getRecurrence(),
                s.isSystem()
        );
    }
}
