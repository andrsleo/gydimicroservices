package com.affiliate.rentals.gydi.calendar.infrastructure.out.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "season_region", schema = "properties")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SeasonRegionJpaEntity {

    @Id
    @Column(name = "code", nullable = false)
    private String code;

    @Column(name = "name", nullable = false)
    private String name;
}
