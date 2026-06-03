package com.affiliate.rentals.gydi.calendar.domain.port.in;

import com.affiliate.rentals.gydi.calendar.application.dto.SeasonDefinitionDto;

import java.util.List;

public interface ListSeasonDefinitionsUseCase {
    List<SeasonDefinitionDto> execute();
}
