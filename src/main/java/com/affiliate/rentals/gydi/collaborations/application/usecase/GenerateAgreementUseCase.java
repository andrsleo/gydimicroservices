package com.affiliate.rentals.gydi.collaborations.application.usecase;

import com.affiliate.rentals.gydi.collaborations.application.port.out.AgreementRepositoryPort;
import com.affiliate.rentals.gydi.collaborations.application.port.out.PitchRepositoryPort;
import com.affiliate.rentals.gydi.collaborations.domain.exception.PitchNotFoundException;
import com.affiliate.rentals.gydi.collaborations.domain.model.AgreementDeliverable;
import com.affiliate.rentals.gydi.collaborations.domain.model.CollaborationAgreement;
import com.affiliate.rentals.gydi.collaborations.domain.model.ContentRight;
import com.affiliate.rentals.gydi.collaborations.domain.model.CounterOffer;
import com.affiliate.rentals.gydi.collaborations.domain.model.CounterOfferDeliverable;
import com.affiliate.rentals.gydi.collaborations.domain.model.CreatorPitch;
import com.affiliate.rentals.gydi.collaborations.domain.model.PitchDeliverable;
import com.affiliate.rentals.gydi.collaborations.domain.model.enums.ContentRightType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Use case to auto-generate a CollaborationAgreement from an accepted pitch.
 *
 * <p>Uses the most recent counter-offer deliverables when available;
 * falls back to the original pitch deliverables otherwise.
 * Adds a default ORGANIC_SOCIAL content right.
 *
 * <p>Called by {@link AcceptPitchUseCase} — not exposed as a REST endpoint directly.
 *
 * @author GYDI Development Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GenerateAgreementUseCase {

    private final PitchRepositoryPort pitchRepository;
    private final AgreementRepositoryPort agreementRepository;

    @Transactional
    public CollaborationAgreement execute(Long pitchId) {
        CreatorPitch pitch = pitchRepository.findById(pitchId)
                .orElseThrow(() -> PitchNotFoundException.withId(pitchId));

        List<AgreementDeliverable> agreementDeliverables = buildDeliverables(pitch);
        List<ContentRight> contentRights = List.of(ContentRight.of(ContentRightType.ORGANIC_SOCIAL));

        CollaborationAgreement agreement = CollaborationAgreement.fromPitch(
                pitch.id(),
                pitch.propertyId(),
                pitch.hostId(),
                pitch.creatorId(),
                pitch.preferredCheckIn(),
                pitch.preferredCheckOut(),
                agreementDeliverables,
                contentRights,
                pitch.compensation()
        );

        CollaborationAgreement saved = agreementRepository.save(agreement);
        log.info("Agreement generated: agreementId={}, pitchId={}", saved.id(), pitchId);
        return saved;
    }

    /**
     * Returns AgreementDeliverables based on the most recent counter-offer deliverables,
     * or falls back to the original pitch deliverables if no counter-offers exist.
     *
     * <p>Note: agreementId is null at creation time — the persistence adapter assigns it
     * after INSERT. Deliverables use null agreementId as a sentinel until the parent
     * agreement is persisted.
     */
    private List<AgreementDeliverable> buildDeliverables(CreatorPitch pitch) {
        List<CounterOffer> counterOffers = pitch.counterOffers();

        if (!counterOffers.isEmpty()) {
            CounterOffer lastOffer = counterOffers.getLast();
            List<CounterOfferDeliverable> counterDeliverables = lastOffer.deliverables();

            if (!counterDeliverables.isEmpty()) {
                return counterDeliverables.stream()
                        .map(cd -> AgreementDeliverable.create(null, cd.type(), cd.quantity()))
                        .toList();
            }
        }

        // Fall back to original pitch deliverables
        return pitch.deliverables().stream()
                .map(pd -> AgreementDeliverable.create(null, pd.type(), pd.quantity()))
                .toList();
    }
}
