package com.affiliate.rentals.gydi.collaborations.infrastructure.out.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.util.List;

@Entity
@Table(schema = "collaborations", name = "pitch_compensation")
@Getter
@Setter
@NoArgsConstructor
public class PitchCompensationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pitch_id", nullable = false, unique = true)
    private PitchEntity pitch;

    @Column(name = "compensation_type", nullable = false, length = 25)
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
}
