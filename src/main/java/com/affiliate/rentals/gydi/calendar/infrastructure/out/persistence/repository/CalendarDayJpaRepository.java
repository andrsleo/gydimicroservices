package com.affiliate.rentals.gydi.calendar.infrastructure.out.persistence.repository;

import com.affiliate.rentals.gydi.calendar.infrastructure.out.persistence.entity.CalendarDayJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface CalendarDayJpaRepository extends JpaRepository<CalendarDayJpaEntity, Long> {

    Optional<CalendarDayJpaEntity> findByPropertyIdAndCalendarDate(Long propertyId, LocalDate date);

    List<CalendarDayJpaEntity> findByPropertyIdAndCalendarDateBetweenOrderByCalendarDateAsc(
            Long propertyId, LocalDate from, LocalDate to);

    @Modifying
    @Query("DELETE FROM CalendarDayJpaEntity c WHERE c.propertyId = :propertyId AND c.calendarDate = :date")
    void deleteByPropertyIdAndCalendarDate(@Param("propertyId") Long propertyId, @Param("date") LocalDate date);

    @Modifying
    @Query("DELETE FROM CalendarDayJpaEntity c WHERE c.bookingId = :bookingId")
    void deleteByBookingId(@Param("bookingId") Long bookingId);

    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN TRUE ELSE FALSE END FROM CalendarDayJpaEntity c " +
           "WHERE c.propertyId = :propertyId AND c.calendarDate BETWEEN :from AND :to AND c.status != 'AVAILABLE'")
    boolean existsNonAvailableInRange(@Param("propertyId") Long propertyId,
                                      @Param("from") LocalDate from,
                                      @Param("to") LocalDate to);
}
