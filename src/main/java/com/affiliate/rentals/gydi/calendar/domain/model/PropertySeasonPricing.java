package com.affiliate.rentals.gydi.calendar.domain.model;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.Objects;

/**
 * Entity representing a property's season price multipliers.
 *
 * <p>Pure Java domain model — no Spring, JPA, or Lombok.</p>
 */
public final class PropertySeasonPricing {

    private static final BigDecimal DEFAULT_HIGH_MULTIPLIER = new BigDecimal("1.50");
    private static final BigDecimal DEFAULT_MEDIUM_MULTIPLIER = new BigDecimal("1.20");
    private static final BigDecimal DEFAULT_LOW_MULTIPLIER = new BigDecimal("0.80");

    private final Long id;
    private final Long propertyId;
    private final SeasonType seasonType;
    private final BigDecimal priceMultiplier;
    private final ZonedDateTime createdAt;
    private final ZonedDateTime updatedAt;

    private PropertySeasonPricing(
            Long id,
            Long propertyId,
            SeasonType seasonType,
            BigDecimal priceMultiplier,
            ZonedDateTime createdAt,
            ZonedDateTime updatedAt) {
        this.id = id;
        this.propertyId = Objects.requireNonNull(propertyId, "propertyId cannot be null");
        this.seasonType = Objects.requireNonNull(seasonType, "seasonType cannot be null");
        this.priceMultiplier = Objects.requireNonNull(priceMultiplier, "priceMultiplier cannot be null");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt cannot be null");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt cannot be null");
    }

    // -------------------------------------------------------------------------
    // Factory methods
    // -------------------------------------------------------------------------

    /**
     * Creates a PropertySeasonPricing with the default multiplier for the given season type:
     * <ul>
     *   <li>HIGH   → 1.50</li>
     *   <li>MEDIUM → 1.20</li>
     *   <li>LOW    → 0.80</li>
     * </ul>
     */
    public static PropertySeasonPricing createDefault(Long propertyId, SeasonType seasonType) {
        BigDecimal defaultMultiplier = switch (seasonType) {
            case HIGH -> DEFAULT_HIGH_MULTIPLIER;
            case MEDIUM -> DEFAULT_MEDIUM_MULTIPLIER;
            case LOW -> DEFAULT_LOW_MULTIPLIER;
        };
        ZonedDateTime now = ZonedDateTime.now();
        return new PropertySeasonPricing(null, propertyId, seasonType, defaultMultiplier, now, now);
    }

    /** Reconstructs a PropertySeasonPricing from persistence. */
    public static PropertySeasonPricing reconstruct(
            Long id,
            Long propertyId,
            SeasonType seasonType,
            BigDecimal multiplier,
            ZonedDateTime createdAt,
            ZonedDateTime updatedAt) {
        return new PropertySeasonPricing(id, propertyId, seasonType, multiplier, createdAt, updatedAt);
    }

    // -------------------------------------------------------------------------
    // Getters
    // -------------------------------------------------------------------------

    public Long getId() { return id; }
    public Long getPropertyId() { return propertyId; }
    public SeasonType getSeasonType() { return seasonType; }
    public BigDecimal getPriceMultiplier() { return priceMultiplier; }
    public ZonedDateTime getCreatedAt() { return createdAt; }
    public ZonedDateTime getUpdatedAt() { return updatedAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PropertySeasonPricing other)) return false;
        return Objects.equals(id, other.id)
                && Objects.equals(propertyId, other.propertyId)
                && seasonType == other.seasonType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, propertyId, seasonType);
    }

    @Override
    public String toString() {
        return "PropertySeasonPricing{id=%d, propertyId=%d, seasonType=%s, multiplier=%s}"
                .formatted(id, propertyId, seasonType, priceMultiplier);
    }
}
