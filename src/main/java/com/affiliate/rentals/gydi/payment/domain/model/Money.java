package com.affiliate.rentals.gydi.payment.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.Objects;

/**
 * Value object representing a monetary amount with currency.
 * <p>
 * Immutable and self-validating following DDD principles.
 * </p>
 */
public final class Money {

    private static final int DEFAULT_SCALE = 2;
    private static final RoundingMode DEFAULT_ROUNDING = RoundingMode.HALF_UP;

    private final BigDecimal amount;
    private final String currencyCode;

    /**
     * Creates a new Money instance.
     *
     * @param amount       Monetary amount
     * @param currencyCode ISO 4217 currency code (e.g., "USD", "EUR")
     * @throws IllegalArgumentException if amount is negative or currency is invalid
     */
    public Money(BigDecimal amount, String currencyCode) {
        validateAmount(amount);
        validateCurrency(currencyCode);

        this.amount = amount.setScale(DEFAULT_SCALE, DEFAULT_ROUNDING);
        this.currencyCode = currencyCode.toUpperCase();
    }

    /**
     * Factory method for convenience.
     */
    public static Money of(BigDecimal amount, String currencyCode) {
        return new Money(amount, currencyCode);
    }

    /**
     * Factory method from string amounts.
     */
    public static Money of(String amount, String currencyCode) {
        return new Money(new BigDecimal(amount), currencyCode);
    }

    /**
     * Adds two Money instances (must have same currency).
     */
    public Money add(Money other) {
        validateSameCurrency(other);
        return new Money(this.amount.add(other.amount), this.currencyCode);
    }

    /**
     * Subtracts another Money from this (must have same currency).
     */
    public Money subtract(Money other) {
        validateSameCurrency(other);
        BigDecimal result = this.amount.subtract(other.amount);
        if (result.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Money cannot be negative after subtraction");
        }
        return new Money(result, this.currencyCode);
    }

    /**
     * Multiplies by a percentage (e.g., 0.05 for 5%).
     */
    public Money multiplyByPercentage(BigDecimal percentage) {
        if (percentage.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Percentage cannot be negative");
        }
        return new Money(this.amount.multiply(percentage), this.currencyCode);
    }

    /**
     * Checks if amount is zero.
     */
    public boolean isZero() {
        return amount.compareTo(BigDecimal.ZERO) == 0;
    }

    /**
     * Checks if amount is positive (> 0).
     */
    public boolean isPositive() {
        return amount.compareTo(BigDecimal.ZERO) > 0;
    }

    // Getters
    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    // Validation
    private void validateAmount(BigDecimal amount) {
        if (amount == null) {
            throw new IllegalArgumentException("Amount cannot be null");
        }
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Amount cannot be negative: " + amount);
        }
    }

    private void validateCurrency(String currencyCode) {
        if (currencyCode == null || currencyCode.isBlank()) {
            throw new IllegalArgumentException("Currency code cannot be null or empty");
        }
        if (currencyCode.length() != 3) {
            throw new IllegalArgumentException("Currency code must be 3 characters (ISO 4217): " + currencyCode);
        }
        try {
            Currency.getInstance(currencyCode.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid currency code: " + currencyCode, e);
        }
    }

    private void validateSameCurrency(Money other) {
        if (!this.currencyCode.equals(other.currencyCode)) {
            throw new IllegalArgumentException(
                String.format("Cannot perform operation on different currencies: %s vs %s",
                    this.currencyCode, other.currencyCode)
            );
        }
    }

    // Equals and HashCode
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Money money = (Money) o;
        return amount.compareTo(money.amount) == 0 &&
            Objects.equals(currencyCode, money.currencyCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(amount, currencyCode);
    }

    @Override
    public String toString() {
        return currencyCode + " " + amount.toPlainString();
    }
}
