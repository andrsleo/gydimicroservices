package com.affiliate.rentals.gydi.collaborations.infrastructure.out.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(schema = "collaborations", name = "counter_offers")
@Getter
@Setter
@NoArgsConstructor
public class CounterOfferEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pitch_id", nullable = false)
    private PitchEntity pitch;

    @Column(name = "round_number", nullable = false)
    private Short roundNumber;

    @Column(name = "offered_by", nullable = false, length = 10)
    private String offeredBy;

    @Column(name = "message")
    private String message;

    @Column(name = "compensation_type", length = 25)
    private String compensationType;

    @Column(name = "nights")
    private Short nights;

    @Column(name = "amount", precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency", length = 3)
    private String currency;

    @Column(name = "booking_commission_pct", precision = 5, scale = 2)
    private BigDecimal bookingCommissionPct;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "experience_items", columnDefinition = "text[]")
    private List<String> experienceItems;

    @Column(name = "created_at", nullable = false, columnDefinition = "TIMESTAMPTZ")
    private OffsetDateTime createdAt;

    @OneToMany(mappedBy = "counterOffer", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<CounterOfferDeliverableEntity> deliverables = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = OffsetDateTime.now();
    }
}
