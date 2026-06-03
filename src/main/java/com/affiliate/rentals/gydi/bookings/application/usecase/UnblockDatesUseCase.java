package com.affiliate.rentals.gydi.bookings.application.usecase;

import com.affiliate.rentals.gydi.bookings.domain.exception.InvalidCalendarOperationException;
import com.affiliate.rentals.gydi.bookings.domain.exception.UnauthorizedPropertyAccessException;
import com.affiliate.rentals.gydi.bookings.domain.model.PropertyCalendar;
import com.affiliate.rentals.gydi.bookings.domain.ports.PropertyCalendarRepositoryPort;
import com.affiliate.rentals.gydi.properties.domain.exception.PropertyNotFoundException;
import com.affiliate.rentals.gydi.properties.domain.model.Property;
import com.affiliate.rentals.gydi.properties.domain.model.PropertyId;
import com.affiliate.rentals.gydi.properties.domain.ports.out.PropertyRepositoryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * Use Case: Unblock a date on a property calendar (HOST only).
 * <p>
 * Business Flow:
 * 1. Verify property ownership
 * 2. Reject unblocking if date is locked by an active booking
 * 3. Delete the calendar entry
 * </p>
 */
@Service
public class UnblockDatesUseCase {

    private static final Logger log = LoggerFactory.getLogger(UnblockDatesUseCase.class);

    private final PropertyCalendarRepositoryPort calendarRepository;
    private final PropertyRepositoryPort propertyRepository;

    public UnblockDatesUseCase(PropertyCalendarRepositoryPort calendarRepository,
                               PropertyRepositoryPort propertyRepository) {
        this.calendarRepository = Objects.requireNonNull(calendarRepository);
        this.propertyRepository = Objects.requireNonNull(propertyRepository);
    }

    @Transactional
    public void execute(Long propertyId, LocalDate dateToUnblock, Long requestingHostId) {
        log.info("Host {} unblocking date {} on property {}", requestingHostId, dateToUnblock, propertyId);

        // 1. Verificar que la propiedad pertenece al host
        Property property = propertyRepository.findById(PropertyId.of(propertyId))
                .orElseThrow(() -> new PropertyNotFoundException(String.valueOf(propertyId)));

        if (!property.getHostId().equals(requestingHostId)) {
            throw new UnauthorizedPropertyAccessException(
                    "Host " + requestingHostId + " does not own property " + propertyId);
        }

        // 2. No permitir desbloquear fechas que fueron bloqueadas por una reserva activa
        List<PropertyCalendar> entries = calendarRepository
                .findByPropertyIdBetween(propertyId, dateToUnblock, dateToUnblock);

        entries.stream()
                .filter(PropertyCalendar::isBlockedByBooking)
                .findFirst()
                .ifPresent(e -> {
                    throw new InvalidCalendarOperationException(
                            "Date " + dateToUnblock + " is blocked by active booking " + e.getBookingId()
                    );
                });

        // 3. Eliminar el bloqueo
        calendarRepository.deleteByPropertyIdAndDate(propertyId, dateToUnblock);

        log.info("Unblocked date {} on property {} for host {}", dateToUnblock, propertyId, requestingHostId);
    }
}
