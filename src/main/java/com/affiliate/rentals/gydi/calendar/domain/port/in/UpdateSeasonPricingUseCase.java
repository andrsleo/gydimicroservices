package com.affiliate.rentals.gydi.calendar.domain.port.in;

import com.affiliate.rentals.gydi.calendar.application.dto.UpdateSeasonPricingRequest;
import com.affiliate.rentals.gydi.calendar.application.dto.SeasonPricingResponse;

public interface UpdateSeasonPricingUseCase {
    SeasonPricingResponse execute(Long propertyId, UpdateSeasonPricingRequest request, Long requestingHostId);
}
