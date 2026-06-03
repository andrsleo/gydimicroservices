package com.affiliate.rentals.gydi.calendar.domain.port.out;

import com.affiliate.rentals.gydi.calendar.application.dto.SeasonRegionDto;

import java.util.List;

public interface SeasonRegionRepositoryPort {
    List<SeasonRegionDto> findAll();
}
