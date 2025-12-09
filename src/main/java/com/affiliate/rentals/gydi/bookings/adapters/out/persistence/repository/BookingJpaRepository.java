package com.affiliate.rentals.gydi.bookings.adapters.out.persistence.repository;

import com.affiliate.rentals.gydi.bookings.adapters.out.persistence.entity.BookingEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * Spring Data JPA Repository for BookingEntity.
 */
@Repository
public interface BookingJpaRepository extends JpaRepository<BookingEntity, Long> {

    /**
     * Finds all bookings for a specific referral link.
     */
    List<BookingEntity> findByReferralLinkId(Long referralLinkId);

    /**
     * Finds active bookings (REQUEST or RESERVED) for a property.
     */
    @Query("""
        SELECT b FROM BookingEntity b
        WHERE b.propertyId = :propertyId
        AND b.status IN ('REQUEST', 'RESERVED')
        ORDER BY b.startDate ASC
        """)
    List<BookingEntity> findActiveBookingsByProperty(@Param("propertyId") Long propertyId);

    /**
     * Finds all bookings by status.
     */
    List<BookingEntity> findByStatus(BookingEntity.BookingStatusEntity status);

    /**
     * Checks if property has any overlapping bookings in RESERVED or FINISHED status.
     * <p>
     * Two date ranges overlap if: (StartA <= EndB) AND (EndA >= StartB)
     * </p>
     */
    @Query("""
        SELECT COUNT(b) > 0
        FROM BookingEntity b
        WHERE b.propertyId = :propertyId
        AND b.status IN ('RESERVED', 'FINISHED')
        AND b.endDate >= :startDate
        AND b.startDate <= :endDate
        """)
    boolean existsOverlappingBooking(
        @Param("propertyId") Long propertyId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );
}
