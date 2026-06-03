package com.affiliate.rentals.gydi.calendar.domain.port.in;

import com.affiliate.rentals.gydi.calendar.application.dto.SeasonDefinitionDto;
import com.affiliate.rentals.gydi.calendar.application.dto.UpdateSeasonDefinitionRequest;

public interface UpdateSeasonDefinitionUseCase {
    SeasonDefinitionDto execute(Long id, UpdateSeasonDefinitionRequest request);
}
