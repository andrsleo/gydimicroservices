package com.affiliate.rentals.gydi.bookings.infrastructure.out.persistence.repository;

import com.affiliate.rentals.gydi.bookings.infrastructure.out.persistence.entity.PropertyCalendarJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for property_calendar table.
 */
@Repository
public interface PropertyCalendarJpaRepository extends JpaRepository<PropertyCalendarJpaEntity, Long> {

    List<PropertyCalendarJpaEntity> findByPropertyIdAndBlockedDateBetweenOrderByBlockedDateAsc(
            Long propertyId, LocalDate from, LocalDate to);

    Optional<PropertyCalendarJpaEntity> findByPropertyIdAndBlockedDate(Long propertyId, LocalDate date);

    boolean existsByPropertyIdAndBlockedDate(Long propertyId, LocalDate date);

    @Query("""
            SELECT COUNT(e) > 0
            FROM PropertyCalendarJpaEntity e
            WHERE e.propertyId = :propertyId
              AND e.blockedDate >= :checkIn
              AND e.blockedDate <= :checkOut
            """)
    boolean existsBlockedDateInRange(
            @Param("propertyId") Long propertyId,
            @Param("checkIn") LocalDate checkIn,
            @Param("checkOut") LocalDate checkOut);

    void deleteByPropertyIdAndBlockedDate(Long propertyId, LocalDate date);

    void deleteByBookingId(Long bookingId);
}
