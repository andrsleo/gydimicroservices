package com.affiliate.rentals.gydi.collaborations.infrastructure.out.persistence.jpa;

import com.affiliate.rentals.gydi.collaborations.infrastructure.out.persistence.entity.PitchEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;

public interface PitchJpaRepository extends JpaRepository<PitchEntity, Long> {

    Page<PitchEntity> findByCreatorIdAndStatus(Long creatorId, String status, Pageable pageable);

    Page<PitchEntity> findByCreatorId(Long creatorId, Pageable pageable);

    Page<PitchEntity> findByHostIdAndStatus(Long hostId, String status, Pageable pageable);

    Page<PitchEntity> findByHostId(Long hostId, Pageable pageable);

    @Query(value = """
            SELECT p.* FROM collaborations.pitches p
            WHERE p.host_id = :hostId
            AND p.property_id = :propertyId
            AND (:status IS NULL OR p.status = :status)
            """, nativeQuery = true)
    Page<PitchEntity> findByHostIdAndPropertyIdAndStatus(
            @Param("hostId") Long hostId,
            @Param("propertyId") Long propertyId,
            @Param("status") String status,
            Pageable pageable);

    @Query(value = """
            SELECT CASE WHEN COUNT(p.id) > 0 THEN true ELSE false END
            FROM collaborations.pitches p
            WHERE p.creator_id = :creatorId
              AND p.property_id = :propertyId
              AND p.status IN ('PENDING', 'COUNTERED', 'ACCEPTED')
            """, nativeQuery = true)
    boolean existsActivePitch(@Param("creatorId") Long creatorId, @Param("propertyId") Long propertyId);

    @Query(value = """
            SELECT COUNT(p.id) FROM collaborations.pitches p
            WHERE p.creator_id = :creatorId
              AND p.created_at >= :since
            """, nativeQuery = true)
    int countByCreatorIdSince(@Param("creatorId") Long creatorId, @Param("since") OffsetDateTime since);

    @Query(value = """
            SELECT p.* FROM collaborations.pitches p
            WHERE p.status IN ('PENDING', 'COUNTERED')
              AND p.expires_at < :now
            """, nativeQuery = true)
    List<PitchEntity> findExpired(@Param("now") OffsetDateTime now);
}
