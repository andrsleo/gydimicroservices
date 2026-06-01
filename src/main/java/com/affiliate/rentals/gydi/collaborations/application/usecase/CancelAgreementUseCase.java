package com.affiliate.rentals.gydi.collaborations.application.usecase;

import com.affiliate.rentals.gydi.collaborations.application.port.out.AgreementRepositoryPort;
import com.affiliate.rentals.gydi.collaborations.domain.event.AgreementCancelledEvent;
import com.affiliate.rentals.gydi.collaborations.domain.exception.AgreementNotFoundException;
import com.affiliate.rentals.gydi.collaborations.domain.exception.CollaborationAccessDeniedException;
import com.affiliate.rentals.gydi.collaborations.domain.model.CollaborationAgreement;
import com.affiliate.rentals.gydi.collaborations.domain.model.enums.OfferedBy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Use case to cancel an active CollaborationAgreement.
 *
 * <p>Only ACTIVE agreements can be cancelled. Either party (host or creator) may cancel.
 * Publishes {@link AgreementCancelledEvent} on success.
 *
 * @author GYDI Development Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CancelAgreementUseCase {

    private final AgreementRepositoryPort agreementRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public void execute(Long agreementId, Long requestingUserId) {
        CollaborationAgreement agreement = agreementRepository.findById(agreementId)
                .orElseThrow(() -> AgreementNotFoundException.withId(agreementId));

        OfferedBy cancelledBy = resolveParty(agreement, requestingUserId);

        agreement.cancel(cancelledBy);
        agreementRepository.save(agreement);

        eventPublisher.publishEvent(new AgreementCancelledEvent(
                agreement.id(),
                agreement.pitchId(),
                agreement.hostId(),
                agreement.creatorId(),
                cancelledBy.name()
        ));

        log.info("Agreement cancelled: agreementId={}, cancelledBy={}", agreementId, cancelledBy);
    }

    private OfferedBy resolveParty(CollaborationAgreement agreement, Long requestingUserId) {
        if (agreement.hostId().equals(requestingUserId)) return OfferedBy.HOST;
        if (agreement.creatorId().equals(requestingUserId)) return OfferedBy.CREATOR;
        throw new CollaborationAccessDeniedException(
                "User " + requestingUserId + " is not a party to agreement " + agreement.id());
    }
}
