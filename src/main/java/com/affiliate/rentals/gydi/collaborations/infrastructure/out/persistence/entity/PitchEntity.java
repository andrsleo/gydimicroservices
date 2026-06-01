package com.affiliate.rentals.gydi.collaborations.infrastructure.out.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(schema = "collaborations", name = "pitches")
@Getter
@Setter
@NoArgsConstructor
public class PitchEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "property_id", nullable = false)
    private Long propertyId;

    @Column(name = "host_id", nullable = false)
    private Long hostId;

    @Column(name = "creator_id", nullable = false)
    private Long creatorId;

    @Column(name = "introduction", nullable = false, length = 1000)
    private String introduction;

    @Column(name = "portfolio_url")
    private String portfolioUrl;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "counter_offer_rounds", nullable = false)
    private Short counterOfferRounds = 0;

    @Column(name = "preferred_check_in", nullable = false)
    private LocalDate preferredCheckIn;

    @Column(name = "preferred_check_out", nullable = false)
    private LocalDate preferredCheckOut;

    @Column(name = "expires_at", columnDefinition = "TIMESTAMPTZ")
    private OffsetDateTime expiresAt;

    @Column(name = "declined_reason")
    private String declinedReason;

    @Column(name = "created_at", nullable = false, columnDefinition = "TIMESTAMPTZ")
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false, columnDefinition = "TIMESTAMPTZ")
    private OffsetDateTime updatedAt;

    @OneToMany(mappedBy = "pitch", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<PitchDeliverableEntity> deliverables = new ArrayList<>();

    @OneToOne(mappedBy = "pitch", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private PitchCompensationEntity compensation;

    @OneToMany(mappedBy = "pitch", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<CounterOfferEntity> counterOffers = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = OffsetDateTime.now();
        if (updatedAt == null) updatedAt = OffsetDateTime.now();
        if (status == null) status = "PENDING";
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
