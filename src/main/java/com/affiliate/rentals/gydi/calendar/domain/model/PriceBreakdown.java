package com.affiliate.rentals.gydi.calendar.domain.model;

import com.affiliate.rentals.gydi.properties.domain.model.Money;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Value object representing the price breakdown for a date range (check-in to check-out).
 *
 * <p>Contains one {@link DayPrice} per night in the range and the total price.
 * Immutable — all collections are unmodifiable.</p>
 */
public final class PriceBreakdown {

    private final LocalDate checkIn;
    private final LocalDate checkOut;
    private final List<DayPrice> days;
    private final Money totalPrice;
    private final String currency;

    private PriceBreakdown(
            LocalDate checkIn,
            LocalDate checkOut,
            List<DayPrice> days,
            Money totalPrice) {
        this.checkIn = Objects.requireNonNull(checkIn, "checkIn cannot be null");
        this.checkOut = Objects.requireNonNull(checkOut, "checkOut cannot be null");
        this.days = Collections.unmodifiableList(
                Objects.requireNonNull(days, "days cannot be null"));
        this.totalPrice = Objects.requireNonNull(totalPrice, "totalPrice cannot be null");
        this.currency = totalPrice.currency().getCurrencyCode();
    }

    // -------------------------------------------------------------------------
    // Factory method
    // -------------------------------------------------------------------------

    /**
     * Creates a PriceBreakdown value object.
     *
     * @param checkIn  first night
     * @param checkOut last night (exclusive — the departure date)
     * @param days     list of per-night prices (one per night between checkIn and checkOut)
     * @param total    total price (sum of all day prices)
     */
    public static PriceBreakdown of(LocalDate checkIn, LocalDate checkOut, List<DayPrice> days, Money total) {
        return new PriceBreakdown(checkIn, checkOut, days, total);
    }

    // -------------------------------------------------------------------------
    // Getters
    // -------------------------------------------------------------------------

    public LocalDate getCheckIn() { return checkIn; }
    public LocalDate getCheckOut() { return checkOut; }
    public List<DayPrice> getDays() { return days; }
    public Money getTotalPrice() { return totalPrice; }
    public String getCurrency() { return currency; }

    // -------------------------------------------------------------------------
    // Nested record
    // -------------------------------------------------------------------------

    /**
     * Per-night price record within a PriceBreakdown.
     */
    public record DayPrice(
            LocalDate date,
            Money price,
            PriceSource priceSource,
            CalendarDayStatus status
    ) {
        public DayPrice {
            Objects.requireNonNull(date, "date cannot be null");
            Objects.requireNonNull(price, "price cannot be null");
            Objects.requireNonNull(priceSource, "priceSource cannot be null");
            Objects.requireNonNull(status, "status cannot be null");
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PriceBreakdown other)) return false;
        return Objects.equals(checkIn, other.checkIn)
                && Objects.equals(checkOut, other.checkOut)
                && Objects.equals(totalPrice, other.totalPrice);
    }

    @Override
    public int hashCode() {
        return Objects.hash(checkIn, checkOut, totalPrice);
    }

    @Override
    public String toString() {
        return "PriceBreakdown{checkIn=%s, checkOut=%s, nights=%d, total=%s}"
                .formatted(checkIn, checkOut, days.size(), totalPrice);
    }
}
