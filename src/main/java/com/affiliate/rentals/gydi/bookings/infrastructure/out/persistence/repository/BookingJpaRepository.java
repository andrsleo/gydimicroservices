package com.affiliate.rentals.gydi.bookings.infrastructure.out.persistence.repository;

import com.affiliate.rentals.gydi.bookings.domain.model.BookingStatus;
import com.affiliate.rentals.gydi.bookings.infrastructure.out.persistence.entity.BookingJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * Spring Data JPA repository for BookingJpaEntity.
 */
@Repository
public interface BookingJpaRepository extends JpaRepository<BookingJpaEntity, Long> {

    List<BookingJpaEntity> findByPropertyId(Long propertyId);

    List<BookingJpaEntity> findByReferralLinkId(Long referralLinkId);

    List<BookingJpaEntity> findByStatus(BookingStatus status);

    @Query("SELECT b FROM BookingJpaEntity b WHERE b.status = 'RESERVED' AND b.checkInDate <= :currentDate")
    List<BookingJpaEntity> findReadyToStart(@Param("currentDate") LocalDate currentDate);

    @Query("SELECT b FROM BookingJpaEntity b WHERE b.status = 'IN_PROGRESS' AND b.checkOutDate <= :currentDate")
    List<BookingJpaEntity> findReadyToFinish(@Param("currentDate") LocalDate currentDate);

    /**
     * Checks if there are any overlapping bookings for a property within the
     * specified date range.
     * <p>
     * ✅ SECURITY FIX: Prevents double-booking attacks.
     * </p>
     * <p>
     * Overlap logic:
     * - Existing booking's check-in is before requested check-out AND
     * - Existing booking's check-out is after requested check-in
     * </p>
     * <p>
     * Only considers active bookings (REQUEST, RESERVED, IN_PROGRESS).
     * Excludes CANCELLED and FINISHED bookings.
     * </p>
     *
     * @param propertyId   the property ID
     * @param checkInDate  the requested check-in date
     * @param checkOutDate the requested check-out date
     * @return true if there are overlapping bookings
     */
    @Query("""
            SELECT CASE WHEN COUNT(b) > 0 THEN true ELSE false END
            FROM BookingJpaEntity b
            WHERE b.propertyId = :propertyId
              AND b.checkInDate < :checkOutDate
              AND b.checkOutDate > :checkInDate
              AND b.status NOT IN ('CANCELLED', 'FINISHED')
            """)
    boolean existsOverlappingBooking(
            @Param("propertyId") Long propertyId,
            @Param("checkInDate") LocalDate checkInDate,
            @Param("checkOutDate") LocalDate checkOutDate);

    @Query(value = "SELECT host_id FROM properties.properties WHERE id = :propertyId", nativeQuery = true)
    Long findHostIdByPropertyId(@Param("propertyId") Long propertyId);

    @Query(value = "SELECT affiliate_id FROM referrals.referral_links WHERE id = :referralLinkId", nativeQuery = true)
    Long findAffiliateIdByReferralLinkId(@Param("referralLinkId") Long referralLinkId);

    @Query(value = "SELECT id FROM users.users WHERE email = 'system-organic@gydi.internal' LIMIT 1", nativeQuery = true)
    Long findOrganicSystemUserId();
}
