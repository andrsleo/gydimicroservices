package com.affiliate.rentals.gydi.bookings.infrastructure.out.persistence.repository;

import com.affiliate.rentals.gydi.bookings.infrastructure.out.persistence.entity.BookingStatusHistoryJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA repository for BookingStatusHistoryJpaEntity.
 */
@Repository
public interface BookingStatusHistoryJpaRepository extends JpaRepository<BookingStatusHistoryJpaEntity, Long> {

    @Query("SELECT h FROM BookingStatusHistoryJpaEntity h WHERE h.booking.id = :bookingId ORDER BY h.createdAt ASC")
    List<BookingStatusHistoryJpaEntity> findByBookingIdOrderByCreatedAtAsc(@Param("bookingId") Long bookingId);

    List<BookingStatusHistoryJpaEntity> findByChangedBy(Long changedBy);
}
