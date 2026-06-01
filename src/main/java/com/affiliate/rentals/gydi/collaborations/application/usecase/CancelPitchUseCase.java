package com.affiliate.rentals.gydi.collaborations.application.usecase;

import com.affiliate.rentals.gydi.collaborations.application.port.out.PitchRepositoryPort;
import com.affiliate.rentals.gydi.collaborations.domain.exception.CollaborationAccessDeniedException;
import com.affiliate.rentals.gydi.collaborations.domain.exception.PitchNotFoundException;
import com.affiliate.rentals.gydi.collaborations.domain.model.CreatorPitch;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Use case to cancel a pitch. Only the creator who submitted the pitch may cancel it.
 *
 * @author GYDI Development Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CancelPitchUseCase {

    private final PitchRepositoryPort pitchRepository;

    @Transactional
    public void execute(Long pitchId, Long requestingUserId) {
        CreatorPitch pitch = pitchRepository.findById(pitchId)
                .orElseThrow(() -> PitchNotFoundException.withId(pitchId));

        if (!pitch.creatorId().equals(requestingUserId)) {
            throw new CollaborationAccessDeniedException(
                    "Only the creator can cancel a pitch. pitchId=" + pitchId);
        }

        pitch.cancel();
        pitchRepository.save(pitch);

        log.info("Pitch cancelled: pitchId={}, creatorId={}", pitchId, requestingUserId);
    }
}
