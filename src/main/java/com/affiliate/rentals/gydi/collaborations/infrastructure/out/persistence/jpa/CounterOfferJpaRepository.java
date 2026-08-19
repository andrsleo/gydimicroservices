package com.affiliate.rentals.gydi.collaborations.infrastructure.out.persistence.jpa;

import com.affiliate.rentals.gydi.collaborations.infrastructure.out.persistence.entity.CounterOfferEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CounterOfferJpaRepository extends JpaRepository<CounterOfferEntity, Long> {

    List<CounterOfferEntity> findByPitchIdOrderByRoundNumberAsc(Long pitchId);

    int countByPitchId(Long pitchId);
}
