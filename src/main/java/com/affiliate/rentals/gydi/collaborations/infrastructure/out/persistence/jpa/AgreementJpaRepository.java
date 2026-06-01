package com.affiliate.rentals.gydi.collaborations.infrastructure.out.persistence.jpa;

import com.affiliate.rentals.gydi.collaborations.infrastructure.out.persistence.entity.AgreementEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AgreementJpaRepository extends JpaRepository<AgreementEntity, Long> {

    Optional<AgreementEntity> findByPitchId(Long pitchId);
}
