package com.affiliate.rentals.gydi.calendar.domain.port.in;

import java.time.LocalDate;
import java.util.List;

public interface BlockDatesUseCase {
    void execute(Long propertyId, List<LocalDate> dates, String notes, Long requestingHostId);
}
