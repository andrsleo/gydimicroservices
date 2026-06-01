package com.affiliate.rentals.gydi.collaborations.application.port.out;

import com.affiliate.rentals.gydi.collaborations.domain.model.CounterOffer;

import java.util.List;
import java.util.Optional;

/**
 * Output port for CounterOffer persistence operations.
 */
public interface CounterOfferRepositoryPort {

    CounterOffer save(CounterOffer counterOffer);

    Optional<CounterOffer> findById(Long id);

    List<CounterOffer> findByPitchId(Long pitchId);

    int countByPitchId(Long pitchId);
}
