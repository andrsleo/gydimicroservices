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
 * Use case to retrieve detailed information about a pitch.
 *
 * <p>Only the host or the creator who is party to the pitch may view it.
 * Returns the domain object; the controller mapper is responsible for DTO conversion.
 *
 * @author GYDI Development Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GetPitchDetailUseCase {

    private final PitchRepositoryPort pitchRepository;

    @Transactional(readOnly = true)
    public CreatorPitch execute(Long pitchId, Long requestingUserId) {
        CreatorPitch pitch = pitchRepository.findById(pitchId)
                .orElseThrow(() -> PitchNotFoundException.withId(pitchId));

        boolean isHost = pitch.hostId().equals(requestingUserId);
        boolean isCreator = pitch.creatorId().equals(requestingUserId);

        if (!isHost && !isCreator) {
            throw new CollaborationAccessDeniedException(
                    "User " + requestingUserId + " is not a party to pitch " + pitchId);
        }

        return pitch;
    }
}
