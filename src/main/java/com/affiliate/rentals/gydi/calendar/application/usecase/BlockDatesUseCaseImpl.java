package com.affiliate.rentals.gydi.calendar.application.usecase;

import com.affiliate.rentals.gydi.bookings.domain.exception.UnauthorizedPropertyAccessException;
import com.affiliate.rentals.gydi.calendar.domain.model.BlockSource;
import com.affiliate.rentals.gydi.calendar.domain.model.CalendarDay;
import com.affiliate.rentals.gydi.calendar.domain.model.CalendarDayStatus;
import com.affiliate.rentals.gydi.calendar.domain.port.in.BlockDatesUseCase;
import com.affiliate.rentals.gydi.calendar.domain.port.out.CalendarDayRepositoryPort;
import com.affiliate.rentals.gydi.properties.domain.exception.PropertyNotFoundException;
import com.affiliate.rentals.gydi.properties.domain.model.Property;
import com.affiliate.rentals.gydi.properties.domain.model.PropertyId;
import com.affiliate.rentals.gydi.properties.domain.ports.out.PropertyRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class BlockDatesUseCaseImpl implements BlockDatesUseCase {

    private final PropertyRepositoryPort propertyRepository;
    private final CalendarDayRepositoryPort calendarDayRepository;

    public BlockDatesUseCaseImpl(
            PropertyRepositoryPort propertyRepository,
            CalendarDayRepositoryPort calendarDayRepository) {
        this.propertyRepository = propertyRepository;
        this.calendarDayRepository = calendarDayRepository;
    }

    @Override
    @Transactional
    public void execute(Long propertyId, List<LocalDate> dates, String notes, Long requestingHostId) {
        Property property = propertyRepository.findById(PropertyId.of(propertyId))
                .orElseThrow(() -> new PropertyNotFoundException(String.valueOf(propertyId)));

        if (!property.getHostId().equals(requestingHostId)) {
            throw new UnauthorizedPropertyAccessException(
                    "Host " + requestingHostId + " does not own property " + propertyId);
        }

        LocalDate today = LocalDate.now();
        List<LocalDate> validDates = dates.stream()
                .filter(date -> !date.isBefore(today))
                .collect(Collectors.toList());

        if (validDates.isEmpty()) {
            return;
        }

        LocalDate minDate = validDates.stream().min(LocalDate::compareTo).orElseThrow();
        LocalDate maxDate = validDates.stream().max(LocalDate::compareTo).orElseThrow();

        Map<LocalDate, CalendarDay> existingMap =
                calendarDayRepository.findByPropertyIdAndDateRangeAsMap(propertyId, minDate, maxDate);

        List<CalendarDay> toSave = new ArrayList<>();
        ZonedDateTime now = ZonedDateTime.now();

        for (LocalDate date : validDates) {
            CalendarDay existing = existingMap.get(date);

            if (existing != null && existing.getStatus() == CalendarDayStatus.RESERVED) {
                continue;
            }

            if (existing != null
                    && (existing.getStatus() == CalendarDayStatus.BLOCKED
                        || existing.getStatus() == CalendarDayStatus.MAINTENANCE)) {
                continue;
            }

            CalendarDay newBlock = CalendarDay.reconstruct(
                    existing != null ? existing.getId() : null,
                    propertyId,
                    date,
                    CalendarDayStatus.BLOCKED,
                    null,
                    BlockSource.MANUAL,
                    null,
                    notes,
                    requestingHostId,
                    existing != null ? existing.getCreatedAt() : now,
                    now);

            toSave.add(newBlock);
        }

        if (!toSave.isEmpty()) {
            calendarDayRepository.saveAll(toSave);
        }
    }
}
