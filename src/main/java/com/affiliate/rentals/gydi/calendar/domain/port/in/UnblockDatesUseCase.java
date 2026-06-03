package com.affiliate.rentals.gydi.calendar.domain.port.in;

import java.time.LocalDate;

public interface UnblockDatesUseCase {
    void execute(Long propertyId, LocalDate date, Long requestingHostId);
}
