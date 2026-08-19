package com.affiliate.rentals.gydi.collaborations.infrastructure.out.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(schema = "collaborations", name = "pitch_deliverables")
@Getter
@Setter
@NoArgsConstructor
public class PitchDeliverableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pitch_id", nullable = false)
    private PitchEntity pitch;

    @Column(name = "deliverable_type", nullable = false, length = 20)
    private String deliverableType;

    @Column(name = "quantity", nullable = false)
    private Short quantity;

    @Column(name = "notes")
    private String notes;
}
