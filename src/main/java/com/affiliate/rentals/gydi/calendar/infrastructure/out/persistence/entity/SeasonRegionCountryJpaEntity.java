package com.affiliate.rentals.gydi.calendar.infrastructure.out.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Objects;

@Entity
@Table(name = "season_region_country", schema = "properties")
@IdClass(SeasonRegionCountryJpaEntity.SeasonRegionCountryId.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SeasonRegionCountryJpaEntity {

    @Id
    @Column(name = "region_code", nullable = false)
    private String regionCode;

    @Id
    @Column(name = "country", nullable = false)
    private String country;

    public static class SeasonRegionCountryId implements Serializable {
        private String regionCode;
        private String country;

        public SeasonRegionCountryId() {}

        public SeasonRegionCountryId(String regionCode, String country) {
            this.regionCode = regionCode;
            this.country = country;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof SeasonRegionCountryId that)) return false;
            return Objects.equals(regionCode, that.regionCode) && Objects.equals(country, that.country);
        }

        @Override
        public int hashCode() {
            return Objects.hash(regionCode, country);
        }
    }
}
