package com.affiliate.rentals.gydi.referrals.infrastructure.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ContentAttributionJpaRepository extends JpaRepository<ContentAttributionJpaEntity, Long> {

    List<ContentAttributionJpaEntity> findByCreatorId(Long creatorId);

    Optional<ContentAttributionJpaEntity> findByBookingId(Long bookingId);

    List<ContentAttributionJpaEntity> findByContentPostId(Long contentPostId);

    /** Count distinct bookings attributed to a specific content post */
    @Query("SELECT COUNT(ca) FROM ContentAttributionJpaEntity ca WHERE ca.contentPostId = :contentPostId AND ca.bookingId IS NOT NULL")
    int countBookingsByContentPostId(@Param("contentPostId") Long contentPostId);

    /** Top creator IDs by booking count for a property, limited to top 5 */
    @Query(value = """
            SELECT ca.creator_id
            FROM referrals.content_attributions ca
            INNER JOIN content.content_posts cp ON cp.id = ca.content_post_id
            WHERE cp.property_id = :propertyId
            AND ca.booking_id IS NOT NULL
            GROUP BY ca.creator_id
            ORDER BY COUNT(ca.booking_id) DESC
            LIMIT 5
            """, nativeQuery = true)
    List<Long> findTopCreatorIdsByPropertyId(@Param("propertyId") Long propertyId);

    /** Count total bookings attributed via any content for a property */
    @Query(value = """
            SELECT COUNT(ca.booking_id)
            FROM referrals.content_attributions ca
            INNER JOIN content.content_posts cp ON cp.id = ca.content_post_id
            WHERE cp.property_id = :propertyId
            AND ca.booking_id IS NOT NULL
            """, nativeQuery = true)
    int countTotalBookingsByPropertyId(@Param("propertyId") Long propertyId);
}
