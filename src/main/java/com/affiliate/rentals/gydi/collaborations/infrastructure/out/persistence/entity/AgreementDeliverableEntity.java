package com.affiliate.rentals.gydi.collaborations.infrastructure.out.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(schema = "collaborations", name = "agreement_deliverables")
@Getter
@Setter
@NoArgsConstructor
public class AgreementDeliverableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agreement_id", nullable = false)
    private AgreementEntity agreement;

    @Column(name = "deliverable_type", nullable = false, length = 20)
    private String deliverableType;

    @Column(name = "quantity", nullable = false)
    private Short quantity;

    @Column(name = "status", nullable = false, length = 25)
    private String status = "PENDING";

    @Column(name = "revision_feedback")
    private String revisionFeedback;

    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "agreement_deliverable_id")
    private List<DeliveryAssetEntity> assets = new ArrayList<>();
}
