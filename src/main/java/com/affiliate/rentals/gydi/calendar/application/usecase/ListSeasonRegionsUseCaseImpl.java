package com.affiliate.rentals.gydi.calendar.application.usecase;

import com.affiliate.rentals.gydi.calendar.application.dto.SeasonRegionDto;
import com.affiliate.rentals.gydi.calendar.domain.port.in.ListSeasonRegionsUseCase;
import com.affiliate.rentals.gydi.calendar.domain.port.out.SeasonRegionRepositoryPort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ListSeasonRegionsUseCaseImpl implements ListSeasonRegionsUseCase {

    private final SeasonRegionRepositoryPort repository;

    public ListSeasonRegionsUseCaseImpl(SeasonRegionRepositoryPort repository) {
        this.repository = repository;
    }

    @Override
    public List<SeasonRegionDto> execute() {
        return repository.findAll();
    }
}
