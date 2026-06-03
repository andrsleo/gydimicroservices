package com.affiliate.rentals.gydi.calendar.domain.port.in;

import com.affiliate.rentals.gydi.calendar.application.dto.SeasonRegionDto;

import java.util.List;

public interface ListSeasonRegionsUseCase {
    List<SeasonRegionDto> execute();
}
