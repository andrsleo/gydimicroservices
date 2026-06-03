package com.affiliate.rentals.gydi.calendar.domain.service;

import com.affiliate.rentals.gydi.calendar.domain.model.CalendarDay;
import com.affiliate.rentals.gydi.calendar.domain.model.CalendarDayStatus;
import com.affiliate.rentals.gydi.calendar.domain.model.PriceBreakdown;
import com.affiliate.rentals.gydi.calendar.domain.model.PriceSource;
import com.affiliate.rentals.gydi.calendar.domain.model.SeasonDefinition;
import com.affiliate.rentals.gydi.calendar.domain.model.SeasonType;
import com.affiliate.rentals.gydi.properties.domain.model.Money;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Domain service that calculates effective prices for individual nights and date ranges.
 *
 * <p>Priority order: custom price > seasonal price > base price.</p>
 *
 * <p>Pure Java — no Spring annotations, no JPA, no Lombok.</p>
 */
public final class PriceCalculatorService {

    /**
     * Per-night price result, combining the resolved Money amount and the source used.
     */
    public record PricedNight(Money price, PriceSource source) {
        public PricedNight {
            Objects.requireNonNull(price, "price cannot be null");
            Objects.requireNonNull(source, "source cannot be null");
        }
    }

    /**
     * Calculates the effective price for a single night.
     * Priority: custom price > seasonal price > base price.
     *
     * @param date         the night to price
     * @param basePrice    the property's base price per night
     * @param calendarDay  the calendar day entry (nullable — null means AVAILABLE with no custom price)
     * @param activeSeason the active season type for this date (nullable — null means no season)
     * @param multipliers  map from SeasonType to price multiplier (from PropertySeasonPricing)
     * @return the effective Money price for this night, plus the PriceSource used
     */
    public PricedNight calculatePriceForDate(
            LocalDate date,
            Money basePrice,
            CalendarDay calendarDay,
            SeasonType activeSeason,
            Map<SeasonType, BigDecimal> multipliers) {

        Objects.requireNonNull(date, "date cannot be null");
        Objects.requireNonNull(basePrice, "basePrice cannot be null");
        Objects.requireNonNull(multipliers, "multipliers cannot be null");

        // 1. Custom price takes highest priority
        if (calendarDay != null && calendarDay.hasCustomPrice()) {
            return new PricedNight(calendarDay.getCustomPrice(), PriceSource.CUSTOM);
        }

        // 2. Seasonal price when a season is active and a multiplier is configured
        if (activeSeason != null && multipliers.containsKey(activeSeason)) {
            BigDecimal multiplier = multipliers.get(activeSeason);
            Money seasonalPrice = basePrice.multiply(multiplier);
            return new PricedNight(seasonalPrice, PriceSource.fromSeasonType(activeSeason));
        }

        // 3. Fall back to base price
        return new PricedNight(basePrice, PriceSource.BASE);
    }

    /**
     * Calculates the total price for a stay (check-in to check-out exclusive).
     * Iterates each night from checkIn (inclusive) to checkOut (exclusive).
     * Uses SeasonDetectorService internally for per-day season detection.
     *
     * @param checkIn        check-in date (first night)
     * @param checkOut       check-out date (last day, not a night — not priced)
     * @param basePrice      the property's base price per night
     * @param country        property's country (for season detection)
     * @param region         property's region (nullable)
     * @param calendarDays   map of date -> CalendarDay for the range (days not in map = AVAILABLE, no custom price)
     * @param seasons        all season definitions for this country
     * @param multipliers    map from SeasonType to price multiplier
     * @param seasonDetector reference to SeasonDetectorService for detecting per-day seasons
     * @return a PriceBreakdown with per-night detail and total
     * @throws IllegalArgumentException if checkIn is not before checkOut
     */
    public PriceBreakdown calculateTotalForRange(
            LocalDate checkIn,
            LocalDate checkOut,
            Money basePrice,
            String country,
            String region,
            Map<LocalDate, CalendarDay> calendarDays,
            List<SeasonDefinition> seasons,
            Map<SeasonType, BigDecimal> multipliers,
            SeasonDetectorService seasonDetector) {

        Objects.requireNonNull(checkIn, "checkIn cannot be null");
        Objects.requireNonNull(checkOut, "checkOut cannot be null");
        Objects.requireNonNull(basePrice, "basePrice cannot be null");
        Objects.requireNonNull(country, "country cannot be null");
        Objects.requireNonNull(calendarDays, "calendarDays cannot be null");
        Objects.requireNonNull(seasons, "seasons cannot be null");
        Objects.requireNonNull(multipliers, "multipliers cannot be null");
        Objects.requireNonNull(seasonDetector, "seasonDetector cannot be null");

        if (!checkIn.isBefore(checkOut)) {
            throw new IllegalArgumentException(
                    "checkIn must be before checkOut, but got checkIn=%s checkOut=%s"
                            .formatted(checkIn, checkOut));
        }

        List<PriceBreakdown.DayPrice> dayPrices = new ArrayList<>();
        Money total = Money.zero(basePrice.currency().getCurrencyCode());

        LocalDate current = checkIn;
        while (current.isBefore(checkOut)) {
            CalendarDay calendarDay = calendarDays.get(current);
            SeasonType activeSeason = seasonDetector.detectSeason(current, country, region, seasons);
            PricedNight pricedNight = calculatePriceForDate(current, basePrice, calendarDay, activeSeason, multipliers);

            CalendarDayStatus status = (calendarDay != null)
                    ? calendarDay.getStatus()
                    : CalendarDayStatus.AVAILABLE;

            dayPrices.add(new PriceBreakdown.DayPrice(current, pricedNight.price(), pricedNight.source(), status));
            total = total.add(pricedNight.price());

            current = current.plusDays(1);
        }

        return PriceBreakdown.of(checkIn, checkOut, dayPrices, total);
    }
}
