package com.affiliate.rentals.gydi.collaborations.application.port.out;

import com.affiliate.rentals.gydi.collaborations.domain.model.CreatorPitch;
import com.affiliate.rentals.gydi.collaborations.domain.model.enums.PitchStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Output port for CreatorPitch persistence operations.
 */
public interface PitchRepositoryPort {

    CreatorPitch save(CreatorPitch pitch);

    Optional<CreatorPitch> findById(Long id);

    /**
     * Creator's own pitches, filtered by optional status.
     */
    Page<CreatorPitch> findByCreatorId(Long creatorId, PitchStatus status, Pageable pageable);

    /**
     * All pitches for properties owned by a specific host, filtered by optional status and propertyId.
     */
    Page<CreatorPitch> findByHostId(Long hostId, PitchStatus status, Long propertyId, Pageable pageable);

    /**
     * Check if an active pitch (PENDING/COUNTERED/ACCEPTED) already exists
     * for this creator + property combination.
     */
    boolean existsActivePitch(Long creatorId, Long propertyId);

    /**
     * Count pitches submitted by a creator in the current calendar month.
     * Used to enforce 10-pitch/month limit.
     */
    int countByCreatorIdSince(Long creatorId, OffsetDateTime since);

    /**
     * Return pitches that are PENDING or COUNTERED and whose expiresAt < now.
     * Used by the expiration scheduler.
     */
    List<CreatorPitch> findExpired(OffsetDateTime now);
}
