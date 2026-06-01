package com.affiliate.rentals.gydi.collaborations.infrastructure.out.persistence.mapper;

import com.affiliate.rentals.gydi.collaborations.domain.model.CounterOffer;
import com.affiliate.rentals.gydi.collaborations.domain.model.CounterOfferDeliverable;
import com.affiliate.rentals.gydi.collaborations.domain.model.CreatorPitch;
import com.affiliate.rentals.gydi.collaborations.domain.model.PitchCompensation;
import com.affiliate.rentals.gydi.collaborations.domain.model.PitchDeliverable;
import com.affiliate.rentals.gydi.collaborations.domain.model.enums.CompensationType;
import com.affiliate.rentals.gydi.collaborations.domain.model.enums.DeliverableType;
import com.affiliate.rentals.gydi.collaborations.domain.model.enums.OfferedBy;
import com.affiliate.rentals.gydi.collaborations.domain.model.enums.PitchStatus;
import com.affiliate.rentals.gydi.collaborations.infrastructure.out.persistence.entity.CounterOfferDeliverableEntity;
import com.affiliate.rentals.gydi.collaborations.infrastructure.out.persistence.entity.CounterOfferEntity;
import com.affiliate.rentals.gydi.collaborations.infrastructure.out.persistence.entity.PitchCompensationEntity;
import com.affiliate.rentals.gydi.collaborations.infrastructure.out.persistence.entity.PitchDeliverableEntity;
import com.affiliate.rentals.gydi.collaborations.infrastructure.out.persistence.entity.PitchEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * Hand-written mapper for CreatorPitch ↔ PitchEntity conversion.
 * Uses static methods following the existing PropertyJpaMapper pattern.
 */
public final class PitchEntityMapper {

    private PitchEntityMapper() {}

    // ── toDomain ─────────────────────────────────────────────────────────────

    public static CreatorPitch toDomain(PitchEntity entity) {
        List<PitchDeliverable> deliverables = entity.getDeliverables() != null
                ? entity.getDeliverables().stream().map(PitchEntityMapper::toDeliverableDomain).toList()
                : List.of();

        List<CounterOffer> counterOffers = entity.getCounterOffers() != null
                ? entity.getCounterOffers().stream()
                        .sorted(java.util.Comparator.comparingInt(CounterOfferEntity::getRoundNumber))
                        .map(PitchEntityMapper::toCounterOfferDomain)
                        .toList()
                : List.of();

        PitchCompensation compensation = entity.getCompensation() != null
                ? toCompensationDomain(entity.getCompensation())
                : null;

        return CreatorPitch.builder()
                .id(entity.getId())
                .propertyId(entity.getPropertyId())
                .hostId(entity.getHostId())
                .creatorId(entity.getCreatorId())
                .introduction(entity.getIntroduction())
                .portfolioUrl(entity.getPortfolioUrl())
                .status(PitchStatus.valueOf(entity.getStatus()))
                .counterOfferRounds(entity.getCounterOfferRounds() != null ? entity.getCounterOfferRounds() : 0)
                .preferredCheckIn(entity.getPreferredCheckIn())
                .preferredCheckOut(entity.getPreferredCheckOut())
                .expiresAt(entity.getExpiresAt())
                .declinedReason(entity.getDeclinedReason())
                .deliverables(deliverables)
                .compensation(compensation)
                .counterOffers(counterOffers)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private static PitchDeliverable toDeliverableDomain(PitchDeliverableEntity e) {
        return PitchDeliverable.reconstitute(
                e.getId(),
                DeliverableType.valueOf(e.getDeliverableType()),
                e.getQuantity() != null ? e.getQuantity() : 0,
                e.getNotes()
        );
    }

    private static PitchCompensation toCompensationDomain(PitchCompensationEntity e) {
        CompensationType type = CompensationType.valueOf(e.getCompensationType().toUpperCase());
        return PitchCompensation.reconstitute(
                type,
                e.getNights() != null ? e.getNights().intValue() : null,
                e.getAmount(),
                e.getCurrency(),
                e.getBookingCommissionPct(),
                e.getExperienceItems()
        );
    }

    private static CounterOffer toCounterOfferDomain(CounterOfferEntity e) {
        List<CounterOfferDeliverable> deliverables = e.getDeliverables() != null
                ? e.getDeliverables().stream().map(PitchEntityMapper::toCounterOfferDeliverableDomain).toList()
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

    private static CounterOfferDeliverable toCounterOfferDeliverableDomain(CounterOfferDeliverableEntity e) {
        return CounterOfferDeliverable.reconstitute(
                e.getId(),
                DeliverableType.valueOf(e.getDeliverableType()),
                e.getQuantity() != null ? e.getQuantity() : 0,
                e.getNotes()
        );
    }

    // ── toEntity ──────────────────────────────────────────────────────────────

    public static PitchEntity toEntity(CreatorPitch domain) {
        PitchEntity entity = new PitchEntity();
        entity.setId(domain.id());
        entity.setPropertyId(domain.propertyId());
        entity.setHostId(domain.hostId());
        entity.setCreatorId(domain.creatorId());
        entity.setIntroduction(domain.introduction());
        entity.setPortfolioUrl(domain.portfolioUrl());
        entity.setStatus(domain.status().name());
        entity.setCounterOfferRounds((short) domain.counterOfferRounds());
        entity.setPreferredCheckIn(domain.preferredCheckIn());
        entity.setPreferredCheckOut(domain.preferredCheckOut());
        entity.setExpiresAt(domain.expiresAt());
        entity.setDeclinedReason(domain.declinedReason());
        entity.setCreatedAt(domain.createdAt());
        entity.setUpdatedAt(domain.updatedAt());

        // Deliverables
        List<PitchDeliverableEntity> deliverableEntities = new ArrayList<>();
        for (PitchDeliverable d : domain.deliverables()) {
            PitchDeliverableEntity de = new PitchDeliverableEntity();
            de.setId(d.id());
            de.setPitch(entity);
            de.setDeliverableType(d.type().name());
            de.setQuantity((short) d.quantity());
            de.setNotes(d.notes());
            deliverableEntities.add(de);
        }
        entity.setDeliverables(deliverableEntities);

        // Compensation
        PitchCompensationEntity compEntity = toCompensationEntity(domain.compensation(), entity);
        entity.setCompensation(compEntity);

        // Counter offers
        List<CounterOfferEntity> counterOfferEntities = new ArrayList<>();
        for (CounterOffer co : domain.counterOffers()) {
            counterOfferEntities.add(toCounterOfferEntity(co, entity));
        }
        entity.setCounterOffers(counterOfferEntities);

        return entity;
    }

    private static PitchCompensationEntity toCompensationEntity(PitchCompensation domain, PitchEntity pitchEntity) {
        PitchCompensationEntity e = new PitchCompensationEntity();
        e.setPitch(pitchEntity);
        e.setCompensationType(domain.type().name().toLowerCase());
        e.setNights(domain.nights() != null ? domain.nights().shortValue() : null);
        e.setAmount(domain.amount());
        e.setCurrency(domain.currency());
        e.setBookingCommissionPct(domain.bookingCommissionPct());
        e.setExperienceItems(domain.experienceItems() != null && !domain.experienceItems().isEmpty()
                ? domain.experienceItems() : null);
        return e;
    }

    private static CounterOfferEntity toCounterOfferEntity(CounterOffer domain, PitchEntity pitchEntity) {
        CounterOfferEntity e = new CounterOfferEntity();
        e.setId(domain.id());
        e.setPitch(pitchEntity);
        e.setRoundNumber((short) domain.roundNumber());
        e.setOfferedBy(domain.offeredBy().name());
        e.setMessage(domain.message());
        e.setCompensationType(domain.compensationType() != null
                ? domain.compensationType().name().toLowerCase() : null);
        e.setNights(domain.nights() != null ? domain.nights().shortValue() : null);
        e.setAmount(domain.amount());
        e.setCurrency(domain.currency());
        e.setBookingCommissionPct(domain.bookingCommissionPct());
        e.setExperienceItems(domain.experienceItems() != null && !domain.experienceItems().isEmpty()
                ? domain.experienceItems() : null);
        e.setCreatedAt(domain.createdAt());

        List<CounterOfferDeliverableEntity> deliverables = new ArrayList<>();
        for (CounterOfferDeliverable d : domain.deliverables()) {
            CounterOfferDeliverableEntity de = new CounterOfferDeliverableEntity();
            de.setId(d.id());
            de.setCounterOffer(e);
            de.setDeliverableType(d.type().name());
            de.setQuantity((short) d.quantity());
            de.setNotes(d.notes());
            deliverables.add(de);
        }
        e.setDeliverables(deliverables);

        return e;
    }
}
