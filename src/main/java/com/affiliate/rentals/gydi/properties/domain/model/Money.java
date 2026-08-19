package com.affiliate.rentals.gydi.properties.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.Objects;

/**
 * Money value object representing monetary values with currency information.
 *
 * <p>This is an immutable value object following Domain-Driven Design (DDD) principles.
 * It provides type-safe monetary operations including addition, subtraction, multiplication,
 * and division. All amounts are stored with 2 decimal places using half-up rounding.
 * Currency operations ensure that only money in the same currency can be combined.</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * Money price = Money.of(99.99, "USD");
 * Money total = price.multiply(3);
 * Money perPerson = total.divide(2);
 * boolean isExpensive = price.isGreaterThan(Money.of(100, "USD"));
 * }</pre>
 *
 * @param amount the monetary amount, never {@code null} or negative
 * @param currency the currency, never {@code null}
 * @author GYDI Development Team
 */
public record Money(BigDecimal amount, Currency currency) {

    /**
     * Compact constructor that validates and normalizes the monetary amount.
     * Ensures amount is non-negative and rounds to 2 decimal places.
     *
     * @throws NullPointerException if amount or currency is {@code null}
     * @throws IllegalArgumentException if amount is negative
     */
    public Money {
        Objects.requireNonNull(amount, "Amount cannot be null");
        Objects.requireNonNull(currency, "Currency cannot be null");
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Amount cannot be negative");
        }
        amount = amount.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Factory method to create Money from a BigDecimal amount and currency code.
     *
     * @param amount the monetary amount
     * @param currencyCode the ISO 4217 currency code (e.g., "USD", "EUR")
     * @return a new Money instance
     * @throws IllegalArgumentException if currencyCode is invalid or amount is negative
     */
    public static Money of(BigDecimal amount, String currencyCode) {
        return new Money(amount, Currency.getInstance(currencyCode));
    }

    /**
     * Factory method to create Money from a double amount and currency code.
     *
     * @param amount the monetary amount
     * @param currencyCode the ISO 4217 currency code (e.g., "USD", "EUR")
     * @return a new Money instance
     * @throws IllegalArgumentException if currencyCode is invalid or amount is negative
     */
    public static Money of(double amount, String currencyCode) {
        return new Money(BigDecimal.valueOf(amount), Currency.getInstance(currencyCode));
    }

    /**
     * Factory method to create a Money instance with zero amount.
     *
     * @param currencyCode the ISO 4217 currency code (e.g., "USD", "EUR")
     * @return a new Money instance with zero amount
     * @throws IllegalArgumentException if currencyCode is invalid
     */
    public static Money zero(String currencyCode) {
        return new Money(BigDecimal.ZERO, Currency.getInstance(currencyCode));
    }

    /**
     * Adds another Money amount to this one and returns a new Money instance.
     * Both amounts must be in the same currency.
     *
     * @param other the Money to add
     * @return a new Money instance with the sum
     * @throws IllegalArgumentException if currencies don't match
     */
    public Money add(Money other) {
        ensureSameCurrency(other);
        return new Money(this.amount.add(other.amount), this.currency);
    }

    /**
     * Subtracts another Money amount from this one and returns a new Money instance.
     * Both amounts must be in the same currency.
     *
     * @param other the Money to subtract
     * @return a new Money instance with the difference
     * @throws IllegalArgumentException if currencies don't match or result is negative
     */
    public Money subtract(Money other) {
        ensureSameCurrency(other);
        return new Money(this.amount.subtract(other.amount), this.currency);
    }

    /**
     * Multiplies this Money amount by a BigDecimal multiplier.
     *
     * @param multiplier the multiplier
     * @return a new Money instance with the product
     */
    public Money multiply(BigDecimal multiplier) {
        return new Money(this.amount.multiply(multiplier), this.currency);
    }

    /**
     * Multiplies this Money amount by a long multiplier.
     *
     * @param multiplier the multiplier
     * @return a new Money instance with the product
     */
    public Money multiply(long multiplier) {
        return multiply(BigDecimal.valueOf(multiplier));
    }

    /**
     * Multiplies this Money amount by a double multiplier.
     *
     * @param multiplier the multiplier
     * @return a new Money instance with the product
     */
    public Money multiply(double multiplier) {
        return multiply(BigDecimal.valueOf(multiplier));
    }

    /**
     * Divides this Money amount by a long divisor using half-up rounding.
     *
     * @param divisor the divisor
     * @return a new Money instance with the quotient
     * @throws IllegalArgumentException if divisor is zero
     */
    public Money divide(long divisor) {
        if (divisor == 0) {
            throw new IllegalArgumentException("Cannot divide by zero");
        }
        return new Money(this.amount.divide(BigDecimal.valueOf(divisor), RoundingMode.HALF_UP), this.currency);
    }

    /**
     * Checks if this Money amount is greater than another Money amount.
     * Both amounts must be in the same currency.
     *
     * @param other the Money to compare against
     * @return {@code true} if this amount is greater, {@code false} otherwise
     * @throws IllegalArgumentException if currencies don't match
     */
    public boolean isGreaterThan(Money other) {
        ensureSameCurrency(other);
        return this.amount.compareTo(other.amount) > 0;
    }

    /**
     * Checks if this Money amount is less than another Money amount.
     * Both amounts must be in the same currency.
     *
     * @param other the Money to compare against
     * @return {@code true} if this amount is less, {@code false} otherwise
     * @throws IllegalArgumentException if currencies don't match
     */
    public boolean isLessThan(Money other) {
        ensureSameCurrency(other);
        return this.amount.compareTo(other.amount) < 0;
    }

    /**
     * Checks if this Money amount is zero.
     *
     * @return {@code true} if the amount is zero, {@code false} otherwise
     */
    public boolean isZero() {
        return this.amount.compareTo(BigDecimal.ZERO) == 0;
    }

    /**
     * Validates that another Money instance has the same currency as this one.
     *
     * @param other the Money to validate
     * @throws IllegalArgumentException if currencies don't match
     */
    private void ensureSameCurrency(Money other) {
        if (!this.currency.equals(other.currency)) {
            throw new IllegalArgumentException(
                    "Cannot operate on different currencies: %s and %s"
                            .formatted(this.currency, other.currency)
            );
        }
    }

    @Override
    public String toString() {
        return "%s %s".formatted(currency.getCurrencyCode(), amount);
    }
}
