package com.affiliate.rentals.gydi.collaborations.infrastructure.out.persistence.adapter;

import com.affiliate.rentals.gydi.collaborations.application.port.out.AgreementRepositoryPort;
import com.affiliate.rentals.gydi.collaborations.domain.model.CollaborationAgreement;
import com.affiliate.rentals.gydi.collaborations.infrastructure.out.persistence.entity.AgreementEntity;
import com.affiliate.rentals.gydi.collaborations.infrastructure.out.persistence.entity.PitchCompensationEntity;
import com.affiliate.rentals.gydi.collaborations.infrastructure.out.persistence.jpa.AgreementJpaRepository;
import com.affiliate.rentals.gydi.collaborations.infrastructure.out.persistence.jpa.PitchCompensationJpaRepository;
import com.affiliate.rentals.gydi.collaborations.infrastructure.out.persistence.mapper.AgreementEntityMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class AgreementRepositoryAdapter implements AgreementRepositoryPort {

    private final AgreementJpaRepository jpaRepository;
    private final PitchCompensationJpaRepository pitchCompensationJpaRepository;

    @Override
    public CollaborationAgreement save(CollaborationAgreement agreement) {
        AgreementEntity entity = AgreementEntityMapper.toEntity(agreement);
        AgreementEntity saved = jpaRepository.save(entity);
        // Load compensation from pitch to reconstitute full domain object
        PitchCompensationEntity compensation = pitchCompensationJpaRepository
                .findByPitchId(saved.getPitchId())
                .orElse(null);
        return AgreementEntityMapper.toDomain(saved, compensation);
    }

    @Override
    public Optional<CollaborationAgreement> findById(Long id) {
        return jpaRepository.findById(id).map(entity -> {
            PitchCompensationEntity compensation = pitchCompensationJpaRepository
                    .findByPitchId(entity.getPitchId())
                    .orElse(null);
            return AgreementEntityMapper.toDomain(entity, compensation);
        });
    }

    @Override
    public Optional<CollaborationAgreement> findByPitchId(Long pitchId) {
        return jpaRepository.findByPitchId(pitchId).map(entity -> {
            PitchCompensationEntity compensation = pitchCompensationJpaRepository
                    .findByPitchId(pitchId)
                    .orElse(null);
            return AgreementEntityMapper.toDomain(entity, compensation);
        });
    }
}
