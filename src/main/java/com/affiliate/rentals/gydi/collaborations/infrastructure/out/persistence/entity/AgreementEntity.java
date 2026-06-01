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
@Table(schema = "collaborations", name = "agreements")
@Getter
@Setter
@NoArgsConstructor
public class AgreementEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "pitch_id", nullable = false, unique = true)
    private Long pitchId;

    @Column(name = "property_id", nullable = false)
    private Long propertyId;

    @Column(name = "host_id", nullable = false)
    private Long hostId;

    @Column(name = "creator_id", nullable = false)
    private Long creatorId;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "check_in_date", nullable = false)
    private LocalDate checkInDate;

    @Column(name = "check_out_date", nullable = false)
    private LocalDate checkOutDate;

    @Column(name = "delivery_deadline", nullable = false)
    private LocalDate deliveryDeadline;

    @Column(name = "posting_deadline", nullable = false)
    private LocalDate postingDeadline;

    @Column(name = "cancellation_policy", nullable = false)
    private String cancellationPolicy;

    @Column(name = "cancelled_by", length = 10)
    private String cancelledBy;

    @Column(name = "cancelled_at", columnDefinition = "TIMESTAMPTZ")
    private OffsetDateTime cancelledAt;

    @Column(name = "created_at", nullable = false, columnDefinition = "TIMESTAMPTZ")
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false, columnDefinition = "TIMESTAMPTZ")
    private OffsetDateTime updatedAt;

    @OneToMany(mappedBy = "agreement", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<AgreementDeliverableEntity> deliverables = new ArrayList<>();

    @OneToMany(mappedBy = "agreement", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<ContentRightEntity> contentRights = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = OffsetDateTime.now();
        if (updatedAt == null) updatedAt = OffsetDateTime.now();
        if (status == null) status = "ACTIVE";
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
