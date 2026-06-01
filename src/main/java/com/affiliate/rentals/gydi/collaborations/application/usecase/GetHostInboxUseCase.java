package com.affiliate.rentals.gydi.collaborations.application.usecase;

import com.affiliate.rentals.gydi.collaborations.application.port.out.PitchRepositoryPort;
import com.affiliate.rentals.gydi.collaborations.domain.model.CreatorPitch;
import com.affiliate.rentals.gydi.collaborations.domain.model.enums.PitchStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Use case to list incoming pitches in the host's inbox,
 * with optional filters for status and specific property.
 *
 * @author GYDI Development Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GetHostInboxUseCase {

    private final PitchRepositoryPort pitchRepository;

    @Transactional(readOnly = true)
    public Page<CreatorPitch> execute(Long hostId, PitchStatus statusFilter, Long propertyId, Pageable pageable) {
        return pitchRepository.findByHostId(hostId, statusFilter, propertyId, pageable);
    }
}
