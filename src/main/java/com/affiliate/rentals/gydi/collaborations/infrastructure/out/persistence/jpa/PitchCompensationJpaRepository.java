package com.affiliate.rentals.gydi.collaborations.infrastructure.out.persistence.jpa;

import com.affiliate.rentals.gydi.collaborations.infrastructure.out.persistence.entity.PitchCompensationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PitchCompensationJpaRepository extends JpaRepository<PitchCompensationEntity, Long> {

    Optional<PitchCompensationEntity> findByPitchId(Long pitchId);
}
