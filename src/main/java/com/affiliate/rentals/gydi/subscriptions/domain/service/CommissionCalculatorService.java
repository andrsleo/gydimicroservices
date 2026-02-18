package com.affiliate.rentals.gydi.subscriptions.domain.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Domain service for calculating commissions in the GYDI platform.
 *
 * <p><strong>Business Model:</strong></p>
 * <ul>
 *   <li><strong>AFFILIATE</strong>: RECEIVES commission FROM platform (2%, 5%, 10%)</li>
 *   <li><strong>HOST</strong>: PAYS commission TO platform (25%, 20%, 15%)</li>
 *   <li><strong>PLATFORM</strong>: Earns profit = (host fee - affiliate commission)</li>
 * </ul>
 *
 * <p><strong>Example (PRO plan, $100 booking):</strong></p>
 * <pre>
 * Host (PRO): Platform CHARGES $20 (20%) → Host receives $80
 * Affiliate (PRO): Platform PAYS $5 (5%) → Affiliate receives $5
 * Platform profit: $20 - $5 = $15
 * </pre>
 *
 * <p>This is a pure domain service with no framework dependencies.
 * All calculations use {@link BigDecimal} for precision in monetary operations.</p>
 *
 * @author GYDI Development Team
 * @version 2.0
 * @since 2026-02-04
 */
public final class CommissionCalculatorService {

    private static final int SCALE = 2; // Decimal places for currency
    private static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_UP;

    /**
     * Calculate affiliate commission (what platform PAYS to affiliate).
     *
     * <p>Affiliate commission is the amount the platform pays to the affiliate
     * for successfully referring a booking.</p>
     *
     * @param bookingAmount Total booking amount (must be non-null and >= 0)
     * @param affiliateCommissionRate Affiliate's commission rate (0.02 for FREE, 0.05 for PRO, 0.10 for ELITE)
     * @return Amount platform pays to affiliate, rounded to 2 decimal places
     * @throws IllegalArgumentException if bookingAmount is null or negative
     * @throws IllegalArgumentException if affiliateCommissionRate is null or not between 0 and 1
     */
    public BigDecimal calculateAffiliateCommission(
            BigDecimal bookingAmount,
            BigDecimal affiliateCommissionRate
    ) {
        validateBookingAmount(bookingAmount);
        validateCommissionRate(affiliateCommissionRate, "affiliateCommissionRate");

        return bookingAmount
                .multiply(affiliateCommissionRate)
                .setScale(SCALE, ROUNDING_MODE);
    }

    /**
     * Calculate platform fee (what platform CHARGES to host).
     *
     * <p>Platform fee is the amount the platform charges to the host
     * for bookings generated through the platform.</p>
     *
     * @param bookingAmount Total booking amount (must be non-null and >= 0)
     * @param hostCommissionRate Host's commission rate (0.25 for FREE, 0.20 for PRO, 0.15 for ELITE)
     * @return Amount platform charges to host, rounded to 2 decimal places
     * @throws IllegalArgumentException if bookingAmount is null or negative
     * @throws IllegalArgumentException if hostCommissionRate is null or not between 0 and 1
     */
    public BigDecimal calculatePlatformFee(
            BigDecimal bookingAmount,
            BigDecimal hostCommissionRate
    ) {
        validateBookingAmount(bookingAmount);
        validateCommissionRate(hostCommissionRate, "hostCommissionRate");

        return bookingAmount
                .multiply(hostCommissionRate)
                .setScale(SCALE, ROUNDING_MODE);
    }

    /**
     * Calculate host net income (what host receives after platform fee).
     *
     * <p>Host net income is the amount the host receives after the platform
     * deducts its commission fee.</p>
     *
     * @param bookingAmount Total booking amount (must be non-null and >= 0)
     * @param hostCommissionRate Host's commission rate (0.25 for FREE, 0.20 for PRO, 0.15 for ELITE)
     * @return Amount host receives after platform fee, rounded to 2 decimal places
     * @throws IllegalArgumentException if bookingAmount is null or negative
     * @throws IllegalArgumentException if hostCommissionRate is null or not between 0 and 1
     */
    public BigDecimal calculateHostNetIncome(
            BigDecimal bookingAmount,
            BigDecimal hostCommissionRate
    ) {
        validateBookingAmount(bookingAmount);
        validateCommissionRate(hostCommissionRate, "hostCommissionRate");

        BigDecimal platformFee = calculatePlatformFee(bookingAmount, hostCommissionRate);
        return bookingAmount
                .subtract(platformFee)
                .setScale(SCALE, ROUNDING_MODE);
    }

    /**
     * Calculate platform profit (platform fee - affiliate commission).
     *
     * <p>Platform profit is the net amount the platform earns from a transaction,
     * calculated as the difference between what it charges the host and what it
     * pays the affiliate.</p>
     *
     * @param bookingAmount Total booking amount (must be non-null and >= 0)
     * @param hostCommissionRate Host's commission rate
     * @param affiliateCommissionRate Affiliate's commission rate
     * @return Platform's net profit, rounded to 2 decimal places
     * @throws IllegalArgumentException if any parameter is null or invalid
     */
    public BigDecimal calculatePlatformProfit(
            BigDecimal bookingAmount,
            BigDecimal hostCommissionRate,
            BigDecimal affiliateCommissionRate
    ) {
        validateBookingAmount(bookingAmount);
        validateCommissionRate(hostCommissionRate, "hostCommissionRate");
        validateCommissionRate(affiliateCommissionRate, "affiliateCommissionRate");

        BigDecimal platformFee = calculatePlatformFee(bookingAmount, hostCommissionRate);
        BigDecimal affiliateCommission = calculateAffiliateCommission(bookingAmount, affiliateCommissionRate);

        return platformFee
                .subtract(affiliateCommission)
                .setScale(SCALE, ROUNDING_MODE);
    }

    /**
     * Calculate complete commission breakdown for a booking.
     *
     * <p>Provides a comprehensive view of all monetary flows in a transaction.</p>
     *
     * @param bookingAmount Total booking amount
     * @param hostCommissionRate Host's commission rate
     * @param affiliateCommissionRate Affiliate's commission rate
     * @return CommissionBreakdown with all calculated amounts
     * @throws IllegalArgumentException if any parameter is null or invalid
     */
    public CommissionBreakdown calculateBreakdown(
            BigDecimal bookingAmount,
            BigDecimal hostCommissionRate,
            BigDecimal affiliateCommissionRate
    ) {
        validateBookingAmount(bookingAmount);
        validateCommissionRate(hostCommissionRate, "hostCommissionRate");
        validateCommissionRate(affiliateCommissionRate, "affiliateCommissionRate");

        BigDecimal platformFee = calculatePlatformFee(bookingAmount, hostCommissionRate);
        BigDecimal affiliateCommission = calculateAffiliateCommission(bookingAmount, affiliateCommissionRate);
        BigDecimal hostNetIncome = calculateHostNetIncome(bookingAmount, hostCommissionRate);
        BigDecimal platformProfit = calculatePlatformProfit(bookingAmount, hostCommissionRate, affiliateCommissionRate);

        return new CommissionBreakdown(
                bookingAmount,
                platformFee,
                affiliateCommission,
                hostNetIncome,
                platformProfit
        );
    }

    // ============================================================================
    // VALIDATION METHODS
    // ============================================================================

    private void validateBookingAmount(BigDecimal bookingAmount) {
        Objects.requireNonNull(bookingAmount, "Booking amount cannot be null");
        if (bookingAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Booking amount cannot be negative: " + bookingAmount);
        }
    }

    private void validateCommissionRate(BigDecimal rate, String fieldName) {
        Objects.requireNonNull(rate, fieldName + " cannot be null");
        if (rate.compareTo(BigDecimal.ZERO) < 0 || rate.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException(
                    fieldName + " must be between 0 and 1, but was: " + rate
            );
        }
    }

    // ============================================================================
    // INNER CLASS: COMMISSION BREAKDOWN
    // ============================================================================

    /**
     * Immutable record representing a complete commission breakdown.
     *
     * @param bookingAmount Total booking amount
     * @param platformFee Amount platform charges to host
     * @param affiliateCommission Amount platform pays to affiliate
     * @param hostNetIncome Amount host receives (bookingAmount - platformFee)
     * @param platformProfit Platform's net profit (platformFee - affiliateCommission)
     */
    public record CommissionBreakdown(
            BigDecimal bookingAmount,
            BigDecimal platformFee,
            BigDecimal affiliateCommission,
            BigDecimal hostNetIncome,
            BigDecimal platformProfit
    ) {
        public CommissionBreakdown {
            Objects.requireNonNull(bookingAmount, "bookingAmount cannot be null");
            Objects.requireNonNull(platformFee, "platformFee cannot be null");
            Objects.requireNonNull(affiliateCommission, "affiliateCommission cannot be null");
            Objects.requireNonNull(hostNetIncome, "hostNetIncome cannot be null");
            Objects.requireNonNull(platformProfit, "platformProfit cannot be null");
        }
    }
}
