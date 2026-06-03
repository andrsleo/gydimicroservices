package com.affiliate.rentals.gydi.calendar.infrastructure.out.persistence.entity;

import com.affiliate.rentals.gydi.calendar.domain.model.SeasonType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "property_season_pricing", schema = "properties")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PropertySeasonPricingJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "property_id", nullable = false)
    private Long propertyId;

    @Enumerated(EnumType.STRING)
    @Column(name = "season_type", nullable = false)
    private SeasonType seasonType;

    @Column(name = "price_multiplier", nullable = false, precision = 10, scale = 4)
    private BigDecimal priceMultiplier;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
