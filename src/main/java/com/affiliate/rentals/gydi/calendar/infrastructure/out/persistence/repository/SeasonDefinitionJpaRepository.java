package com.affiliate.rentals.gydi.calendar.infrastructure.out.persistence.repository;

import com.affiliate.rentals.gydi.calendar.infrastructure.out.persistence.entity.SeasonDefinitionJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface SeasonDefinitionJpaRepository extends JpaRepository<SeasonDefinitionJpaEntity, Long> {

    /**
     * Resolves all season definitions applicable to a given country via scope hierarchy:
     * GLOBAL + REGION (where country is a member) + COUNTRY (exact match) + SUBREGION (exact match).
     */
    @Query(value = """
            SELECT sd.* FROM properties.season_definition sd
            WHERE sd.scope = 'GLOBAL'
               OR (sd.scope = 'REGION' AND sd.region_code IN (
                       SELECT src.region_code
                       FROM properties.season_region_country src
                       WHERE LOWER(src.country) = LOWER(:country)
                   ))
               OR (sd.scope IN ('COUNTRY', 'SUBREGION')
                   AND LOWER(sd.country) = LOWER(:country))
            ORDER BY
                CASE sd.scope
                    WHEN 'GLOBAL'    THEN 1
                    WHEN 'REGION'    THEN 2
                    WHEN 'COUNTRY'   THEN 3
                    WHEN 'SUBREGION' THEN 4
                END,
                sd.start_date ASC
            """, nativeQuery = true)
    List<SeasonDefinitionJpaEntity> findApplicableByCountry(@Param("country") String country);

    @Query(value = """
            SELECT sd.* FROM properties.season_definition sd
            WHERE (
                sd.scope = 'GLOBAL'
               OR (sd.scope = 'REGION' AND sd.region_code IN (
                       SELECT src.region_code
                       FROM properties.season_region_country src
                       WHERE LOWER(src.country) = LOWER(:country)
                   ))
               OR (sd.scope IN ('COUNTRY', 'SUBREGION')
                   AND LOWER(sd.country) = LOWER(:country))
            )
            AND sd.start_date <= :endDate
            AND sd.end_date   >= :startDate
            ORDER BY sd.start_date ASC
            """, nativeQuery = true)
    List<SeasonDefinitionJpaEntity> findApplicableByCountryAndDateRange(
            @Param("country") String country,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);
}
