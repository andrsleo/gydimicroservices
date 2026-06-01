package com.affiliate.rentals.gydi.collaborations.application.usecase;

import com.affiliate.rentals.gydi.collaborations.application.port.out.AgreementRepositoryPort;
import com.affiliate.rentals.gydi.collaborations.domain.exception.AgreementNotFoundException;
import com.affiliate.rentals.gydi.collaborations.domain.exception.CollaborationAccessDeniedException;
import com.affiliate.rentals.gydi.collaborations.domain.model.CollaborationAgreement;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Use case to retrieve a CollaborationAgreement by ID.
 *
 * <p>The requesting user must be either the host or creator party to the agreement.
 *
 * @author GYDI Development Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GetAgreementUseCase {

    private final AgreementRepositoryPort agreementRepository;

    @Transactional(readOnly = true)
    public CollaborationAgreement execute(Long agreementId, Long requestingUserId) {
        CollaborationAgreement agreement = agreementRepository.findById(agreementId)
                .orElseThrow(() -> AgreementNotFoundException.withId(agreementId));

        verifyParty(agreement, requestingUserId);

        log.debug("Agreement retrieved: agreementId={}, requestingUser={}", agreementId, requestingUserId);
        return agreement;
    }

    private void verifyParty(CollaborationAgreement agreement, Long requestingUserId) {
        boolean isHost = agreement.hostId().equals(requestingUserId);
        boolean isCreator = agreement.creatorId().equals(requestingUserId);
        if (!isHost && !isCreator) {
            throw new CollaborationAccessDeniedException(
                    "User " + requestingUserId + " is not a party to agreement " + agreement.id());
        }
    }
}
