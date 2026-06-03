package com.affiliate.rentals.gydi.calendar.application.usecase;

import com.affiliate.rentals.gydi.bookings.domain.exception.UnauthorizedPropertyAccessException;
import com.affiliate.rentals.gydi.calendar.domain.model.CalendarDay;
import com.affiliate.rentals.gydi.calendar.domain.model.CalendarDayStatus;
import com.affiliate.rentals.gydi.calendar.domain.port.in.RemoveCustomPriceUseCase;
import com.affiliate.rentals.gydi.calendar.domain.port.out.CalendarDayRepositoryPort;
import com.affiliate.rentals.gydi.properties.domain.exception.PropertyNotFoundException;
import com.affiliate.rentals.gydi.properties.domain.model.Property;
import com.affiliate.rentals.gydi.properties.domain.model.PropertyId;
import com.affiliate.rentals.gydi.properties.domain.ports.out.PropertyRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;

@Service
public class RemoveCustomPriceUseCaseImpl implements RemoveCustomPriceUseCase {

    private final PropertyRepositoryPort propertyRepository;
    private final CalendarDayRepositoryPort calendarDayRepository;

    public RemoveCustomPriceUseCaseImpl(
            PropertyRepositoryPort propertyRepository,
            CalendarDayRepositoryPort calendarDayRepository) {
        this.propertyRepository = propertyRepository;
        this.calendarDayRepository = calendarDayRepository;
    }

    @Override
    @Transactional
    public void execute(Long propertyId, LocalDate date, Long requestingHostId) {
        Property property = propertyRepository.findById(PropertyId.of(propertyId))
                .orElseThrow(() -> new PropertyNotFoundException(String.valueOf(propertyId)));

        if (!property.getHostId().equals(requestingHostId)) {
            throw new UnauthorizedPropertyAccessException(
                    "Host " + requestingHostId + " does not own property " + propertyId);
        }

        Optional<CalendarDay> existing = calendarDayRepository.findByPropertyIdAndDate(propertyId, date);

        if (existing.isEmpty() || !existing.get().hasCustomPrice()) {
            return;
        }

        CalendarDay updated = existing.get().removeCustomPrice();

        if (updated.getStatus() == CalendarDayStatus.AVAILABLE
                && !updated.hasCustomPrice()
                && updated.getBlockSource() == null) {
            calendarDayRepository.delete(propertyId, date);
        } else {
            calendarDayRepository.save(updated);
        }
    }
}
