package com.affiliate.rentals.gydi.bookings.application.usecase;

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
 * Use Case: Block dates on a property calendar (HOST only).
 * <p>
 * Business Flow:
 * 1. Verify property exists and belongs to the requesting host
 * 2. Filter out past dates
 * 3. Create MANUAL blocks in the calendar
 * </p>
 */
@Service
public class BlockDatesUseCase {

    private static final Logger log = LoggerFactory.getLogger(BlockDatesUseCase.class);

    private final PropertyCalendarRepositoryPort calendarRepository;
    private final PropertyRepositoryPort propertyRepository;

    public BlockDatesUseCase(PropertyCalendarRepositoryPort calendarRepository,
                             PropertyRepositoryPort propertyRepository) {
        this.calendarRepository = Objects.requireNonNull(calendarRepository);
        this.propertyRepository = Objects.requireNonNull(propertyRepository);
    }

    @Transactional
    public void execute(Long propertyId, List<LocalDate> datesToBlock, Long requestingHostId) {
        log.info("Host {} blocking {} dates on property {}", requestingHostId, datesToBlock.size(), propertyId);

        // 1. Verificar que la propiedad pertenece al host
        Property property = propertyRepository.findById(PropertyId.of(propertyId))
                .orElseThrow(() -> new PropertyNotFoundException(String.valueOf(propertyId)));

        if (!property.getHostId().equals(requestingHostId)) {
            throw new UnauthorizedPropertyAccessException(
                    "Host " + requestingHostId + " does not own property " + propertyId);
        }

        // 2. Filtrar fechas pasadas
        List<LocalDate> validDates = datesToBlock.stream()
                .filter(d -> !d.isBefore(LocalDate.now()))
                .toList();

        if (validDates.isEmpty()) {
            log.warn("No valid future dates to block for property {}", propertyId);
            return;
        }

        // 3. Crear bloqueos manuales
        List<PropertyCalendar> entries = validDates.stream()
                .map(date -> PropertyCalendar.blockManual(propertyId, date, requestingHostId))
                .toList();

        calendarRepository.saveAll(entries);

        log.info("Blocked {} dates on property {} for host {}", entries.size(), propertyId, requestingHostId);
    }
}
