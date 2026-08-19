package com.affiliate.rentals.gydi.collaborations.application.usecase;

import com.affiliate.rentals.gydi.collaborations.application.port.out.CounterOfferRepositoryPort;
import com.affiliate.rentals.gydi.collaborations.application.port.out.PitchRepositoryPort;
import com.affiliate.rentals.gydi.collaborations.application.usecase.command.CounterOfferResult;
import com.affiliate.rentals.gydi.collaborations.application.usecase.command.CreateCounterOfferCommand;
import com.affiliate.rentals.gydi.collaborations.domain.event.CounterOfferReceivedEvent;
import com.affiliate.rentals.gydi.collaborations.domain.exception.CollaborationAccessDeniedException;
import com.affiliate.rentals.gydi.collaborations.domain.exception.PitchNotFoundException;
import com.affiliate.rentals.gydi.collaborations.domain.model.CounterOffer;
import com.affiliate.rentals.gydi.collaborations.domain.model.CreatorPitch;
import com.affiliate.rentals.gydi.collaborations.domain.model.enums.OfferedBy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Use case to send a counter-offer during pitch negotiation.
 *
 * <p>Validates:
 * <ul>
 *   <li>Pitch exists and is in a state that allows counter-offers</li>
 *   <li>It is the requesting user's turn to respond</li>
 *   <li>The 3-round cap has not been exceeded (enforced by domain)</li>
 * </ul>
 *
 * @author GYDI Development Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CreateCounterOfferUseCase {

    private final PitchRepositoryPort pitchRepository;
    private final CounterOfferRepositoryPort counterOfferRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public CounterOfferResult execute(CreateCounterOfferCommand command) {
        CreatorPitch pitch = pitchRepository.findById(command.pitchId())
                .orElseThrow(() -> PitchNotFoundException.withId(command.pitchId()));

        if (!pitch.canCounterOffer()) {
            throw new IllegalStateException(
                    "Counter-offer not allowed. Status=" + pitch.status()
                            + ", rounds=" + pitch.counterOfferRounds());
        }

        verifyTurn(pitch, command.requestingUserId(), command.offeredBy());

        int roundNumber = pitch.counterOfferRounds() + 1;

        CounterOffer counterOffer = CounterOffer.create(
                pitch.id(),
                roundNumber,
                command.offeredBy(),
                command.message(),
                command.compensationType(),
                command.nights(),
                command.amount(),
                command.currency(),
                command.bookingCommissionPct(),
                command.experienceItems(),
                command.deliverables()
        );

        CounterOffer savedCounterOffer = counterOfferRepository.save(counterOffer);

        // Domain method validates the 3-round cap and resets expiration clock
        pitch.addCounterOffer(savedCounterOffer);
        CreatorPitch savedPitch = pitchRepository.save(pitch);

        Long recipientId = command.offeredBy() == OfferedBy.HOST
                ? savedPitch.creatorId()
                : savedPitch.hostId();

        eventPublisher.publishEvent(new CounterOfferReceivedEvent(
                savedPitch.id(),
                savedCounterOffer.id(),
                roundNumber,
                command.offeredBy().name(),
                recipientId
        ));

        log.info("Counter-offer sent: pitchId={}, round={}, offeredBy={}",
                savedPitch.id(), roundNumber, command.offeredBy());

        return new CounterOfferResult(
                savedPitch.id(),
                savedCounterOffer.id(),
                roundNumber,
                savedPitch.status().name(),
                savedPitch.canCounterOffer(),
                savedPitch.expiresAt()
        );
    }

    private void verifyTurn(CreatorPitch pitch, Long requestingUserId, OfferedBy offeredBy) {
        boolean isHost = pitch.hostId().equals(requestingUserId);
        boolean isCreator = pitch.creatorId().equals(requestingUserId);

        if (!isHost && !isCreator) {
            throw new CollaborationAccessDeniedException(
                    "User " + requestingUserId + " is not a party to pitch " + pitch.id());
        }

        OfferedBy nextResponder = pitch.nextResponder();
        if (nextResponder != offeredBy) {
            throw new CollaborationAccessDeniedException(
                    "It is not your turn to counter-offer. Next responder: " + nextResponder);
        }

        // Also ensure the claimed offeredBy role matches the user's actual role
        if (offeredBy == OfferedBy.HOST && !isHost) {
            throw new CollaborationAccessDeniedException("You are not the host of this pitch");
        }
        if (offeredBy == OfferedBy.CREATOR && !isCreator) {
            throw new CollaborationAccessDeniedException("You are not the creator of this pitch");
        }
    }
}
