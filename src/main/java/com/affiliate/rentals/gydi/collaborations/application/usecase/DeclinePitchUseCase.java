package com.affiliate.rentals.gydi.collaborations.application.usecase;

import com.affiliate.rentals.gydi.collaborations.application.port.out.PitchRepositoryPort;
import com.affiliate.rentals.gydi.collaborations.application.usecase.command.DeclinePitchCommand;
import com.affiliate.rentals.gydi.collaborations.application.usecase.command.DeclinePitchResult;
import com.affiliate.rentals.gydi.collaborations.domain.event.PitchDeclinedEvent;
import com.affiliate.rentals.gydi.collaborations.domain.exception.CollaborationAccessDeniedException;
import com.affiliate.rentals.gydi.collaborations.domain.exception.PitchNotFoundException;
import com.affiliate.rentals.gydi.collaborations.domain.model.CreatorPitch;
import com.affiliate.rentals.gydi.collaborations.domain.model.enums.OfferedBy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Use case to decline a pitch (or the current counter-offer state).
 *
 * <p>Verifies the requesting user is the correct next responder,
 * transitions the pitch to DECLINED, and publishes {@link PitchDeclinedEvent}.
 *
 * @author GYDI Development Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeclinePitchUseCase {

    private final PitchRepositoryPort pitchRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public DeclinePitchResult execute(DeclinePitchCommand command) {
        CreatorPitch pitch = pitchRepository.findById(command.pitchId())
                .orElseThrow(() -> PitchNotFoundException.withId(command.pitchId()));

        verifyResponder(pitch, command.requestingUserId());

        pitch.decline(command.reason());
        CreatorPitch saved = pitchRepository.save(pitch);

        eventPublisher.publishEvent(
                new PitchDeclinedEvent(saved.id(), saved.hostId(), saved.creatorId(), saved.declinedReason()));

        log.info("Pitch declined: pitchId={}, by userId={}", saved.id(), command.requestingUserId());
        return new DeclinePitchResult(saved.id(), saved.status());
    }

    private void verifyResponder(CreatorPitch pitch, Long requestingUserId) {
        boolean isHost = pitch.hostId().equals(requestingUserId);
        boolean isCreator = pitch.creatorId().equals(requestingUserId);

        if (!isHost && !isCreator) {
            throw new CollaborationAccessDeniedException(
                    "User " + requestingUserId + " is not a party to pitch " + pitch.id());
        }

        OfferedBy nextResponder = pitch.nextResponder();
        boolean isHostTurn = nextResponder == OfferedBy.HOST && isHost;
        boolean isCreatorTurn = nextResponder == OfferedBy.CREATOR && isCreator;

        if (!isHostTurn && !isCreatorTurn) {
            throw new CollaborationAccessDeniedException(
                    "It is not your turn to respond. Next responder: " + nextResponder);
        }
    }
}
