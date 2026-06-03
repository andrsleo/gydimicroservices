package com.affiliate.rentals.gydi.calendar.application.usecase;

import com.affiliate.rentals.gydi.bookings.domain.exception.UnauthorizedPropertyAccessException;
import com.affiliate.rentals.gydi.calendar.domain.exception.InvalidCustomPriceException;
import com.affiliate.rentals.gydi.calendar.domain.model.CalendarDay;
import com.affiliate.rentals.gydi.calendar.domain.model.CalendarDayStatus;
import com.affiliate.rentals.gydi.calendar.domain.port.in.SetCustomPriceUseCase;
import com.affiliate.rentals.gydi.calendar.domain.port.out.CalendarDayRepositoryPort;
import com.affiliate.rentals.gydi.properties.domain.exception.PropertyNotFoundException;
import com.affiliate.rentals.gydi.properties.domain.model.Money;
import com.affiliate.rentals.gydi.properties.domain.model.Property;
import com.affiliate.rentals.gydi.properties.domain.model.PropertyId;
import com.affiliate.rentals.gydi.properties.domain.ports.out.PropertyRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Optional;

@Service
public class SetCustomPriceUseCaseImpl implements SetCustomPriceUseCase {

    private final PropertyRepositoryPort propertyRepository;
    private final CalendarDayRepositoryPort calendarDayRepository;

    public SetCustomPriceUseCaseImpl(
            PropertyRepositoryPort propertyRepository,
            CalendarDayRepositoryPort calendarDayRepository) {
        this.propertyRepository = propertyRepository;
        this.calendarDayRepository = calendarDayRepository;
    }

    @Override
    @Transactional
    public void execute(Long propertyId, LocalDate date, Money price, Long requestingHostId) {
        if (price.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidCustomPriceException("Price must be positive");
        }

        Property property = propertyRepository.findById(PropertyId.of(propertyId))
                .orElseThrow(() -> new PropertyNotFoundException(String.valueOf(propertyId)));

        if (!property.getHostId().equals(requestingHostId)) {
            throw new UnauthorizedPropertyAccessException(
                    "Host " + requestingHostId + " does not own property " + propertyId);
        }

        Optional<CalendarDay> existing = calendarDayRepository.findByPropertyIdAndDate(propertyId, date);

        CalendarDay updatedDay;
        if (existing.isPresent()) {
            updatedDay = existing.get().withCustomPrice(price);
        } else {
            ZonedDateTime now = ZonedDateTime.now();
            updatedDay = CalendarDay.reconstruct(
                    null,
                    propertyId,
                    date,
                    CalendarDayStatus.AVAILABLE,
                    price,
                    null,
                    null,
                    null,
                    requestingHostId,
                    now,
                    now);
        }

        calendarDayRepository.save(updatedDay);
    }
}
