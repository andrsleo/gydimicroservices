package com.affiliate.rentals.gydi.bookings.application.usecase;

import com.affiliate.rentals.gydi.bookings.domain.model.PropertyCalendar;
import com.affiliate.rentals.gydi.bookings.domain.ports.PropertyCalendarRepositoryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * Use Case: Get blocked dates for a property calendar (public read).
 * <p>
 * Returns all blocked dates in a given range for display in the booking calendar UI.
 * </p>
 */
@Service
public class GetBlockedDatesUseCase {

    private static final Logger log = LoggerFactory.getLogger(GetBlockedDatesUseCase.class);

    private final PropertyCalendarRepositoryPort calendarRepository;

    public GetBlockedDatesUseCase(PropertyCalendarRepositoryPort calendarRepository) {
        this.calendarRepository = Objects.requireNonNull(calendarRepository);
    }

    public List<LocalDate> execute(Long propertyId, LocalDate from, LocalDate to) {
        log.debug("Getting blocked dates for property {} from {} to {}", propertyId, from, to);

        return calendarRepository
                .findByPropertyIdBetween(propertyId, from, to)
                .stream()
                .map(PropertyCalendar::getBlockedDate)
                .toList();
    }
}
