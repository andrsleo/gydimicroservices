package com.affiliate.rentals.gydi.collaborations.domain.model;

import com.affiliate.rentals.gydi.collaborations.domain.model.enums.CompensationType;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

/**
 * Value object representing the compensation structure of a pitch.
 * Pure Java — no framework annotations.
 */
public final class PitchCompensation {

    private final CompensationType type;
    private final Integer nights;
    private final BigDecimal amount;
    private final String currency;
    private final BigDecimal bookingCommissionPct;
    private final List<String> experienceItems;

    private PitchCompensation(CompensationType type, Integer nights, BigDecimal amount,
                               String currency, BigDecimal bookingCommissionPct,
                               List<String> experienceItems) {
        this.type = Objects.requireNonNull(type, "compensation type must not be null");
        this.nights = nights;
        this.amount = amount;
        this.currency = currency;
        this.bookingCommissionPct = bookingCommissionPct;
        this.experienceItems = experienceItems != null ? List.copyOf(experienceItems) : List.of();
    }

    public static PitchCompensation freeStay(int nights) {
        return new PitchCompensation(CompensationType.FREE_STAY, nights, null, null, null, null);
    }

    public static PitchCompensation cash(BigDecimal amount, String currency) {
        Objects.requireNonNull(amount, "amount must not be null");
        Objects.requireNonNull(currency, "currency must not be null");
        return new PitchCompensation(CompensationType.CASH, null, amount, currency, null, null);
    }

    public static PitchCompensation hybrid(int nights, BigDecimal amount, String currency) {
        Objects.requireNonNull(amount, "amount must not be null");
        Objects.requireNonNull(currency, "currency must not be null");
        return new PitchCompensation(CompensationType.HYBRID, nights, amount, currency, null, null);
    }

    public static PitchCompensation affiliate(BigDecimal commissionPct) {
        Objects.requireNonNull(commissionPct, "commissionPct must not be null");
        return new PitchCompensation(CompensationType.AFFILIATE, null, null, null, commissionPct, null);
    }

    public static PitchCompensation experienceExchange(List<String> items) {
        Objects.requireNonNull(items, "items must not be null");
        return new PitchCompensation(CompensationType.EXPERIENCE_EXCHANGE, null, null, null, null, items);
    }

    public static PitchCompensation reconstitute(CompensationType type, Integer nights, BigDecimal amount,
                                                  String currency, BigDecimal bookingCommissionPct,
                                                  List<String> experienceItems) {
        return new PitchCompensation(type, nights, amount, currency, bookingCommissionPct, experienceItems);
    }

    public CompensationType type() { return type; }
    public Integer nights() { return nights; }
    public BigDecimal amount() { return amount; }
    public String currency() { return currency; }
    public BigDecimal bookingCommissionPct() { return bookingCommissionPct; }
    public List<String> experienceItems() { return experienceItems; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PitchCompensation that)) return false;
        return type == that.type && Objects.equals(nights, that.nights)
                && Objects.equals(amount, that.amount) && Objects.equals(currency, that.currency)
                && Objects.equals(bookingCommissionPct, that.bookingCommissionPct)
                && Objects.equals(experienceItems, that.experienceItems);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, nights, amount, currency, bookingCommissionPct, experienceItems);
    }
}
