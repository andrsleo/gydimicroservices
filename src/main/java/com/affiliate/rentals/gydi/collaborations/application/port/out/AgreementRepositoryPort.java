package com.affiliate.rentals.gydi.collaborations.application.port.out;

import com.affiliate.rentals.gydi.collaborations.domain.model.CollaborationAgreement;

import java.util.Optional;

/**
 * Output port for CollaborationAgreement persistence operations.
 */
public interface AgreementRepositoryPort {

    CollaborationAgreement save(CollaborationAgreement agreement);

    Optional<CollaborationAgreement> findById(Long id);

    Optional<CollaborationAgreement> findByPitchId(Long pitchId);
}
