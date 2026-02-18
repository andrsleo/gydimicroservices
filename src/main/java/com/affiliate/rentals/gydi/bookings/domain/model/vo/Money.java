package com.affiliate.rentals.gydi.bookings.domain.model.vo;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.Objects;
import java.util.Set;

/**
 * Value Object representing monetary amount with currency.
 * <p>
 * Immutable and validates business rules:
 * - Amount must be positive for bookings
 * - Currency must be supported (USD, EUR, GBP, MXN)
 * - Scale is always 2 decimal places
 * </p>
 */
public final class Money {
    private static final Set<String> SUPPORTED_CURRENCIES = Set.of("USD", "EUR", "GBP", "MXN");
    private static final int SCALE = 2;

    private final BigDecimal amount;
    private final String currency;

    private Money(BigDecimal amount, String currency) {
        this.amount = amount.setScale(SCALE, RoundingMode.HALF_UP);
        this.currency = currency;
    }

    /**
     * Creates Money with validation.
     *
     * @param amount monetary amount (must be positive)
     * @param currency ISO 4217 currency code (USD, EUR, GBP, MXN)
     * @return validated Money
     * @throws IllegalArgumentException if validation fails
     */
    public static Money of(BigDecimal amount, String currency) {
        validateAmount(amount);
        validateCurrency(currency);
        return new Money(amount, currency.toUpperCase());
    }

    /**
     * Creates Money in USD (default currency).
     *
     * @param amount monetary amount (must be positive)
     * @return Money in USD
     */
    public static Money usd(BigDecimal amount) {
        return of(amount, "USD");
    }

    private static void validateAmount(BigDecimal amount) {
        if (amount == null) {
            throw new IllegalArgumentException("Amount cannot be null");
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive, got: " + amount);
        }
    }

    private static void validateCurrency(String currency) {
        if (currency == null || currency.isBlank()) {
            throw new IllegalArgumentException("Currency is required");
        }
        String upperCurrency = currency.toUpperCase();
        if (!SUPPORTED_CURRENCIES.contains(upperCurrency)) {
            throw new IllegalArgumentException(
                String.format("Unsupported currency: %s. Supported: %s", currency, SUPPORTED_CURRENCIES)
            );
        }
    }

    /**
     * Checks if amount is positive.
     *
     * @return true if amount > 0
     */
    public boolean isPositive() {
        return amount.compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * Multiplies amount by a factor (useful for commission calculations).
     *
     * @param multiplier multiplication factor
     * @return new Money with multiplied amount
     */
    public Money multiply(BigDecimal multiplier) {
        return new Money(amount.multiply(multiplier), currency);
    }

    /**
     * Adds two Money amounts (must have same currency).
     *
     * @param other Money to add
     * @return new Money with sum
     * @throws IllegalArgumentException if currencies don't match
     */
    public Money add(Money other) {
        if (!currency.equals(other.currency)) {
            throw new IllegalArgumentException(
                String.format("Cannot add different currencies: %s and %s", currency, other.currency)
            );
        }
        return new Money(amount.add(other.amount), currency);
    }

    // Getters
    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Money money = (Money) o;
        return amount.compareTo(money.amount) == 0 &&
               Objects.equals(currency, money.currency);
    }

    @Override
    public int hashCode() {
        return Objects.hash(amount, currency);
    }

    @Override
    public String toString() {
        return String.format("%s %.2f", currency, amount);
    }
}
