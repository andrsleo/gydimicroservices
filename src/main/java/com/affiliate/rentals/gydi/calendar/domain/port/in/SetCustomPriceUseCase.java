package com.affiliate.rentals.gydi.calendar.domain.port.in;

import com.affiliate.rentals.gydi.properties.domain.model.Money;

import java.time.LocalDate;

public interface SetCustomPriceUseCase {
    void execute(Long propertyId, LocalDate date, Money price, Long requestingHostId);
}
