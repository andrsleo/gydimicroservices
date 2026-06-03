package com.affiliate.rentals.gydi.calendar.domain.port.out;

import com.affiliate.rentals.gydi.calendar.domain.model.CalendarDay;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface CalendarDayRepositoryPort {
    CalendarDay save(CalendarDay day);
    void saveAll(List<CalendarDay> days);
    Optional<CalendarDay> findByPropertyIdAndDate(Long propertyId, LocalDate date);
    List<CalendarDay> findByPropertyIdAndDateRange(Long propertyId, LocalDate from, LocalDate to);
    Map<LocalDate, CalendarDay> findByPropertyIdAndDateRangeAsMap(Long propertyId, LocalDate from, LocalDate to);
    void delete(Long propertyId, LocalDate date);
    void deleteByBookingId(Long bookingId);
    boolean hasAnyNonAvailableDate(Long propertyId, LocalDate checkIn, LocalDate checkOut);
}
