package com.affiliate.rentals.gydi.bookings.domain.ports;

import com.affiliate.rentals.gydi.bookings.domain.model.PropertyCalendar;
import java.time.LocalDate;
import java.util.List;

/**
 * Output port for PropertyCalendar persistence.
 * <p>
 * Defines the contract for storing and querying property availability blocks.
 * Implemented by infrastructure adapters (JPA).
 * </p>
 */
public interface PropertyCalendarRepositoryPort {

    void save(PropertyCalendar entry);
    void saveAll(List<PropertyCalendar> entries);

    List<PropertyCalendar> findByPropertyIdBetween(Long propertyId,
                                                    LocalDate from,
                                                    LocalDate to);

    boolean isDateBlocked(Long propertyId, LocalDate date);

    /**
     * Returns true if ANY date in the range [checkIn, checkOut] is blocked.
     */
    boolean hasAnyBlockedDate(Long propertyId, LocalDate checkIn, LocalDate checkOut);

    void deleteByPropertyIdAndDate(Long propertyId, LocalDate date);

    /**
     * Eliminar todos los bloqueos de una reserva (ej. si se cancela).
     */
    void deleteByBookingId(Long bookingId);
}
