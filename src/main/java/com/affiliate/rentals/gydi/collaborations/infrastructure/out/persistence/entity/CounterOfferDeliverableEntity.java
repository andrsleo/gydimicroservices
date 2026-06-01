package com.affiliate.rentals.gydi.collaborations.infrastructure.out.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(schema = "collaborations", name = "counter_offer_deliverables")
@Getter
@Setter
@NoArgsConstructor
public class CounterOfferDeliverableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "counter_offer_id", nullable = false)
    private CounterOfferEntity counterOffer;

    @Column(name = "deliverable_type", nullable = false, length = 20)
    private String deliverableType;

    @Column(name = "quantity", nullable = false)
    private Short quantity;

    @Column(name = "notes")
    private String notes;
}
