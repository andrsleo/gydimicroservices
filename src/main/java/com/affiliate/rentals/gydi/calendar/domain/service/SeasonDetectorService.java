package com.affiliate.rentals.gydi.calendar.domain.service;

import com.affiliate.rentals.gydi.calendar.domain.model.SeasonDefinition;
import com.affiliate.rentals.gydi.calendar.domain.model.SeasonType;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Domain service that detects which season type is active for a given date, country and region.
 *
 * <p>Pure Java — no Spring annotations, no JPA, no Lombok.</p>
 */
public final class SeasonDetectorService {

    /**
     * Detects the active season for a given date, country and optional region.
     * If multiple seasons overlap, priority order is: HIGH > MEDIUM > LOW.
     * Returns null if no season applies (price falls back to base price).
     *
     * @param date     the date to check
     * @param country  the property's country
     * @param region   the property's region (nullable — if null, only country-wide seasons apply)
     * @param seasons  all season definitions for this country (pre-filtered from repository)
     * @return the highest-priority SeasonType active on the given date, or null if none
     */
    public SeasonType detectSeason(LocalDate date, String country, String region, List<SeasonDefinition> seasons) {
        Objects.requireNonNull(date, "date cannot be null");
        Objects.requireNonNull(country, "country cannot be null");
        Objects.requireNonNull(seasons, "seasons cannot be null");

        return seasons.stream()
                .filter(season -> season.isActiveOn(date))
                .filter(season -> season.getCountry() != null && season.getCountry().equalsIgnoreCase(country))
                .filter(season -> season.getRegion() == null
                        || season.getRegion().equalsIgnoreCase(region))
                .map(SeasonDefinition::getSeasonType)
                .min(Comparator.comparingInt(SeasonDetectorService::priorityOf))
                .orElse(null);
    }

    /**
     * Returns the priority index for a SeasonType.
     * Lower index = higher priority (HIGH = 0, MEDIUM = 1, LOW = 2).
     */
    private static int priorityOf(SeasonType seasonType) {
        return switch (seasonType) {
            case HIGH -> 0;
            case MEDIUM -> 1;
            case LOW -> 2;
        };
    }
}
