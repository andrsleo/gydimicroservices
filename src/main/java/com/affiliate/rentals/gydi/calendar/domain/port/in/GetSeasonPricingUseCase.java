package com.affiliate.rentals.gydi.calendar.domain.port.in;

import com.affiliate.rentals.gydi.calendar.application.dto.SeasonPricingResponse;

public interface GetSeasonPricingUseCase {
    SeasonPricingResponse execute(Long propertyId);
}
