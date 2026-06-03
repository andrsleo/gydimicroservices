package com.affiliate.rentals.gydi.calendar.domain.model;

import java.time.LocalDate;
import java.time.MonthDay;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Objects;

/**
 * Entity representing a season period (e.g., High Season Dec–Jan).
 *
 * <p>Supports geographic scope hierarchy:</p>
 * <ul>
 *   <li>GLOBAL     — applies to all countries</li>
 *   <li>REGION     — applies to a named group of countries (e.g., LATAM)</li>
 *   <li>COUNTRY    — applies to one specific country</li>
 *   <li>SUBREGION  — applies to a region within a country</li>
 * </ul>
 *
 * <p>Pure Java domain model — no Spring, JPA, or Lombok.</p>
 */
public final class SeasonDefinition {

    private final Long id;
    private final String name;
    private final SeasonType seasonType;
    /** Scope: GLOBAL | REGION | COUNTRY | SUBREGION */
    private final String scope;
    /** Null for GLOBAL scope; country name for COUNTRY/SUBREGION scope. */
    private final String country;
    /** Null unless scope = REGION. Points to season_region.code. */
    private final String regionCode;
    /** Sub-national region name (used when scope = SUBREGION). */
    private final String region;
    private final LocalDate startDate;
    private final LocalDate endDate;
    private final String recurrence;
    private final boolean isSystem;
    private final ZonedDateTime createdAt;
    private final ZonedDateTime updatedAt;

    private SeasonDefinition(
            Long id,
            String name,
            SeasonType seasonType,
            String scope,
            String country,
            String regionCode,
            String region,
            LocalDate startDate,
            LocalDate endDate,
            String recurrence,
            boolean isSystem,
            ZonedDateTime createdAt,
            ZonedDateTime updatedAt) {
        this.id = id;
        this.name = Objects.requireNonNull(name, "name cannot be null");
        this.seasonType = Objects.requireNonNull(seasonType, "seasonType cannot be null");
        this.scope = Objects.requireNonNull(scope, "scope cannot be null");
        this.country = country;
        this.regionCode = regionCode;
        this.region = region;
        this.startDate = Objects.requireNonNull(startDate, "startDate cannot be null");
        this.endDate = Objects.requireNonNull(endDate, "endDate cannot be null");
        this.recurrence = Objects.requireNonNull(recurrence, "recurrence cannot be null");
        this.isSystem = isSystem;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt cannot be null");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt cannot be null");
    }

    // -------------------------------------------------------------------------
    // Factory methods
    // -------------------------------------------------------------------------

    /** Reconstructs a SeasonDefinition from persistence. */
    public static SeasonDefinition reconstruct(
            Long id,
            String name,
            SeasonType seasonType,
            String scope,
            String country,
            String regionCode,
            String region,
            LocalDate startDate,
            LocalDate endDate,
            String recurrence,
            boolean isSystem,
            ZonedDateTime createdAt,
            ZonedDateTime updatedAt) {
        return new SeasonDefinition(
                id, name, seasonType, scope, country, regionCode, region,
                startDate, endDate, recurrence, isSystem,
                createdAt, updatedAt);
    }

    /** Creates a new (non-system) SeasonDefinition. id will be assigned by persistence. */
    public static SeasonDefinition create(
            String name,
            SeasonType seasonType,
            String scope,
            String country,
            String regionCode,
            String region,
            LocalDate startDate,
            LocalDate endDate,
            String recurrence) {
        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
        return new SeasonDefinition(
                null, name, seasonType, scope, country, regionCode, region,
                startDate, endDate, recurrence, false, now, now);
    }

    // -------------------------------------------------------------------------
    // Business methods
    // -------------------------------------------------------------------------

    /**
     * Returns true if this season definition is active on the given date.
     *
     * <p>For YEARLY recurrence, only month and day are compared (year is ignored).
     * Cross-year seasons (e.g., Dec 15 – Jan 10) are handled by checking
     * whether the date falls on or after the start OR on or before the end
     * (wrap-around logic).</p>
     *
     * <p>For NONE recurrence, the full date is compared.</p>
     */
    public boolean isActiveOn(LocalDate date) {
        Objects.requireNonNull(date, "date cannot be null");

        if ("YEARLY".equalsIgnoreCase(recurrence)) {
            MonthDay startMD = MonthDay.of(startDate.getMonth(), startDate.getDayOfMonth());
            MonthDay endMD = MonthDay.of(endDate.getMonth(), endDate.getDayOfMonth());
            MonthDay targetMD = MonthDay.of(date.getMonth(), date.getDayOfMonth());

            if (startMD.compareTo(endMD) <= 0) {
                return !targetMD.isBefore(startMD) && !targetMD.isAfter(endMD);
            } else {
                return !targetMD.isBefore(startMD) || !targetMD.isAfter(endMD);
            }
        }

        return !date.isBefore(startDate) && !date.isAfter(endDate);
    }

    // -------------------------------------------------------------------------
    // Getters
    // -------------------------------------------------------------------------

    public Long getId() { return id; }
    public String getName() { return name; }
    public SeasonType getSeasonType() { return seasonType; }
    public String getScope() { return scope; }
    public String getCountry() { return country; }
    public String getRegionCode() { return regionCode; }
    public String getRegion() { return region; }
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getEndDate() { return endDate; }
    public String getRecurrence() { return recurrence; }
    public boolean isSystem() { return isSystem; }
    public ZonedDateTime getCreatedAt() { return createdAt; }
    public ZonedDateTime getUpdatedAt() { return updatedAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SeasonDefinition other)) return false;
        return Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "SeasonDefinition{id=%d, name='%s', type=%s, scope=%s, recurrence=%s}"
                .formatted(id, name, seasonType, scope, recurrence);
    }
}
