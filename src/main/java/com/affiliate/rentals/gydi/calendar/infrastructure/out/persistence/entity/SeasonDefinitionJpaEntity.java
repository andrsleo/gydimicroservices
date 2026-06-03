package com.affiliate.rentals.gydi.calendar.infrastructure.out.persistence.entity;

import com.affiliate.rentals.gydi.calendar.domain.model.SeasonType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(name = "season_definition", schema = "properties")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SeasonDefinitionJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "season_type", nullable = false)
    private SeasonType seasonType;

    /** GLOBAL | REGION | COUNTRY | SUBREGION */
    @Column(name = "scope", nullable = false)
    private String scope;

    /** Nullable for GLOBAL and REGION scope. */
    @Column(name = "country")
    private String country;

    /** Populated when scope = REGION; references season_region.code. */
    @Column(name = "region_code")
    private String regionCode;

    /** Sub-national region name (used when scope = SUBREGION). */
    @Column(name = "region")
    private String region;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "recurrence", nullable = false)
    private String recurrence;

    @Column(name = "is_system", nullable = false)
    private boolean isSystem;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
