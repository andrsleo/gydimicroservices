package com.affiliate.rentals.gydi.calendar.domain.port.in;

import com.affiliate.rentals.gydi.calendar.application.dto.CreateSeasonDefinitionRequest;
import com.affiliate.rentals.gydi.calendar.application.dto.SeasonDefinitionDto;

public interface CreateSeasonDefinitionUseCase {
    SeasonDefinitionDto execute(CreateSeasonDefinitionRequest request);
}
