package com.affiliate.rentals.gydi.collaborations.infrastructure.out.persistence.adapter;

import com.affiliate.rentals.gydi.collaborations.application.port.out.CounterOfferRepositoryPort;
import com.affiliate.rentals.gydi.collaborations.domain.model.CounterOffer;
import com.affiliate.rentals.gydi.collaborations.infrastructure.out.persistence.entity.CounterOfferEntity;
import com.affiliate.rentals.gydi.collaborations.infrastructure.out.persistence.entity.CounterOfferDeliverableEntity;
import com.affiliate.rentals.gydi.collaborations.infrastructure.out.persistence.entity.PitchEntity;
import com.affiliate.rentals.gydi.collaborations.infrastructure.out.persistence.jpa.CounterOfferJpaRepository;
import com.affiliate.rentals.gydi.collaborations.infrastructure.out.persistence.jpa.PitchJpaRepository;
import com.affiliate.rentals.gydi.collaborations.domain.model.CounterOfferDeliverable;
import com.affiliate.rentals.gydi.collaborations.domain.model.enums.CompensationType;
import com.affiliate.rentals.gydi.collaborations.domain.model.enums.DeliverableType;
import com.affiliate.rentals.gydi.collaborations.domain.model.enums.OfferedBy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class CounterOfferRepositoryAdapter implements CounterOfferRepositoryPort {

    private final CounterOfferJpaRepository jpaRepository;
    private final PitchJpaRepository pitchJpaRepository;

    @Override
    public CounterOffer save(CounterOffer counterOffer) {
        CounterOfferEntity entity = toEntity(counterOffer);
        CounterOfferEntity saved = jpaRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<CounterOffer> findById(Long id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public List<CounterOffer> findByPitchId(Long pitchId) {
        return jpaRepository.findByPitchIdOrderByRoundNumberAsc(pitchId).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public int countByPitchId(Long pitchId) {
        return jpaRepository.countByPitchId(pitchId);
    }

    private CounterOffer toDomain(CounterOfferEntity e) {
        List<CounterOfferDeliverable> deliverables = e.getDeliverables() != null
                ? e.getDeliverables().stream().map(this::toDeliverableDomain).toList()
                : List.of();

        CompensationType compensationType = e.getCompensationType() != null
                ? CompensationType.valueOf(e.getCompensationType().toUpperCase())
                : null;

        return CounterOffer.builder()
                .id(e.getId())
                .pitchId(e.getPitch().getId())
                .roundNumber(e.getRoundNumber() != null ? e.getRoundNumber() : 0)
                .offeredBy(OfferedBy.valueOf(e.getOfferedBy()))
                .message(e.getMessage())
                .compensationType(compensationType)
                .nights(e.getNights() != null ? e.getNights().intValue() : null)
                .amount(e.getAmount())
                .currency(e.getCurrency())
                .bookingCommissionPct(e.getBookingCommissionPct())
                .experienceItems(e.getExperienceItems())
                .deliverables(deliverables)
                .createdAt(e.getCreatedAt())
                .build();
    }

    private CounterOfferDeliverable toDeliverableDomain(CounterOfferDeliverableEntity e) {
        return CounterOfferDeliverable.reconstitute(
                e.getId(),
                DeliverableType.valueOf(e.getDeliverableType()),
                e.getQuantity() != null ? e.getQuantity() : 0,
                e.getNotes()
        );
    }

    private CounterOfferEntity toEntity(CounterOffer domain) {
        PitchEntity pitchRef = pitchJpaRepository.getReferenceById(domain.pitchId());
        CounterOfferEntity entity = new CounterOfferEntity();
        entity.setId(domain.id());
        entity.setPitch(pitchRef);
        entity.setRoundNumber((short) domain.roundNumber());
        entity.setOfferedBy(domain.offeredBy().name());
        entity.setMessage(domain.message());
        entity.setCompensationType(domain.compensationType() != null
                ? domain.compensationType().name().toLowerCase() : null);
        entity.setNights(domain.nights() != null ? domain.nights().shortValue() : null);
        entity.setAmount(domain.amount());
        entity.setCurrency(domain.currency());
        entity.setBookingCommissionPct(domain.bookingCommissionPct());
        entity.setExperienceItems(domain.experienceItems() != null && !domain.experienceItems().isEmpty()
                ? domain.experienceItems() : null);
        entity.setCreatedAt(domain.createdAt());

        List<CounterOfferDeliverableEntity> deliverables = new ArrayList<>();
        for (CounterOfferDeliverable d : domain.deliverables()) {
            CounterOfferDeliverableEntity de = new CounterOfferDeliverableEntity();
            de.setId(d.id());
            de.setCounterOffer(entity);
            de.setDeliverableType(d.type().name());
            de.setQuantity((short) d.quantity());
            de.setNotes(d.notes());
            deliverables.add(de);
        }
        entity.setDeliverables(deliverables);

        return entity;
    }
}
