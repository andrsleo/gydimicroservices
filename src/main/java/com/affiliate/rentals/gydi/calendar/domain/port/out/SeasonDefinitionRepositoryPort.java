package com.affiliate.rentals.gydi.calendar.domain.port.out;

import com.affiliate.rentals.gydi.calendar.domain.model.SeasonDefinition;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface SeasonDefinitionRepositoryPort {

    /** Resolves all seasons applicable to a country using scope hierarchy. */
    List<SeasonDefinition> findByCountry(String country);

    /** Resolves seasons applicable to a country within a date range. */
    List<SeasonDefinition> findByCountryAndDateRange(String country, LocalDate startDate, LocalDate endDate);

    /** Returns all season definitions (for admin listing). */
    List<SeasonDefinition> findAll();

    Optional<SeasonDefinition> findById(Long id);

    SeasonDefinition save(SeasonDefinition seasonDefinition);

    void deleteById(Long id);

    boolean existsById(Long id);
}
