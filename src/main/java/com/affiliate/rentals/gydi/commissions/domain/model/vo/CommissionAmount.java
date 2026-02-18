package com.affiliate.rentals.gydi.commissions.domain.model.vo;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Value Object representing commission amount with rate and currency.
 * <p>
 * Immutable and validates business rules on construction.
 * </p>
 */
public final class CommissionAmount {
    private final BigDecimal bookingAmount;
    private final BigDecimal commissionRate;
    private final BigDecimal commissionAmount;
    private final String currency;

    private CommissionAmount(
        BigDecimal bookingAmount,
        BigDecimal commissionRate,
        BigDecimal commissionAmount,
        String currency
    ) {
        this.bookingAmount = Objects.requireNonNull(bookingAmount, "Booking amount cannot be null");
        this.commissionRate = Objects.requireNonNull(commissionRate, "Commission rate cannot be null");
        this.commissionAmount = Objects.requireNonNull(commissionAmount, "Commission amount cannot be null");
        this.currency = Objects.requireNonNullElse(currency, "USD");

        validate();
    }

    /**
     * Creates a CommissionAmount by calculating from booking amount and rate.
     */
    public static CommissionAmount calculate(
        BigDecimal bookingAmount,
        BigDecimal commissionRate,
        String currency
    ) {
        Objects.requireNonNull(bookingAmount, "Booking amount cannot be null");
        Objects.requireNonNull(commissionRate, "Commission rate cannot be null");

        BigDecimal commissionAmount = bookingAmount
            .multiply(commissionRate)
            .setScale(2, java.math.RoundingMode.HALF_UP);

        return new CommissionAmount(bookingAmount, commissionRate, commissionAmount, currency);
    }

    /**
     * Reconstitutes CommissionAmount from database values.
     */
    public static CommissionAmount reconstitute(
        BigDecimal bookingAmount,
        BigDecimal commissionRate,
        BigDecimal commissionAmount,
        String currency
    ) {
        return new CommissionAmount(bookingAmount, commissionRate, commissionAmount, currency);
    }

    private void validate() {
        if (bookingAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Booking amount must be positive");
        }
        if (commissionRate.compareTo(BigDecimal.ZERO) < 0 || commissionRate.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException("Commission rate must be between 0 and 1");
        }
        if (commissionAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Commission amount cannot be negative");
        }
        if (commissionAmount.compareTo(bookingAmount) > 0) {
            throw new IllegalArgumentException("Commission amount cannot exceed booking amount");
        }
        if (!isSupportedCurrency(currency)) {
            throw new IllegalArgumentException("Unsupported currency: " + currency);
        }
    }

    private boolean isSupportedCurrency(String currency) {
        return currency != null &&
               (currency.equals("USD") || currency.equals("EUR") ||
                currency.equals("GBP") || currency.equals("MXN"));
    }

    public BigDecimal getBookingAmount() {
        return bookingAmount;
    }

    public BigDecimal getCommissionRate() {
        return commissionRate;
    }

    public BigDecimal getCommissionAmount() {
        return commissionAmount;
    }

    public String getCurrency() {
        return currency;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CommissionAmount that = (CommissionAmount) o;
        return Objects.equals(bookingAmount, that.bookingAmount) &&
               Objects.equals(commissionRate, that.commissionRate) &&
               Objects.equals(commissionAmount, that.commissionAmount) &&
               Objects.equals(currency, that.currency);
    }

    @Override
    public int hashCode() {
        return Objects.hash(bookingAmount, commissionRate, commissionAmount, currency);
    }

    @Override
    public String toString() {
        return String.format("%s %s (%.2f%% of %s %s)",
            commissionAmount, currency,
            commissionRate.multiply(BigDecimal.valueOf(100)),
            bookingAmount, currency
        );
    }
}
