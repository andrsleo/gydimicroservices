package com.affiliate.rentals.gydi.subscriptions.domain.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link CommissionCalculatorService}.
 *
 * <p>Tests cover:</p>
 * <ul>
 *   <li>Affiliate commission calculations (platform PAYS to affiliate)</li>
 *   <li>Platform fee calculations (platform CHARGES to host)</li>
 *   <li>Host net income calculations</li>
 *   <li>Platform profit calculations</li>
 *   <li>Complete breakdown calculations</li>
 *   <li>Edge cases (zero amounts, null values, invalid rates)</li>
 * </ul>
 *
 * @author GYDI Development Team
 */
@DisplayName("CommissionCalculatorService Tests")
class CommissionCalculatorServiceTest {

    private CommissionCalculatorService calculatorService;

    // Commission rates for each plan
    private static final BigDecimal FREE_AFFILIATE_RATE = new BigDecimal("0.02"); // 2%
    private static final BigDecimal PRO_AFFILIATE_RATE = new BigDecimal("0.05");  // 5%
    private static final BigDecimal ELITE_AFFILIATE_RATE = new BigDecimal("0.10"); // 10%

    private static final BigDecimal FREE_HOST_RATE = new BigDecimal("0.25"); // 25%
    private static final BigDecimal PRO_HOST_RATE = new BigDecimal("0.20");  // 20%
    private static final BigDecimal ELITE_HOST_RATE = new BigDecimal("0.15"); // 15%

    @BeforeEach
    void setUp() {
        calculatorService = new CommissionCalculatorService();
    }

    // ============================================================================
    // AFFILIATE COMMISSION TESTS (Platform PAYS to Affiliate)
    // ============================================================================

    @Nested
    @DisplayName("Affiliate Commission Calculations")
    class AffiliateCommissionTests {

        @Test
        @DisplayName("Should calculate affiliate commission for FREE plan (2%)")
        void shouldCalculateAffiliateCommission_FreePlan() {
            // Given
            BigDecimal bookingAmount = new BigDecimal("100.00");

            // When
            BigDecimal commission = calculatorService.calculateAffiliateCommission(
                    bookingAmount,
                    FREE_AFFILIATE_RATE
            );

            // Then
            assertEquals(new BigDecimal("2.00"), commission);
        }

        @Test
        @DisplayName("Should calculate affiliate commission for PRO plan (5%)")
        void shouldCalculateAffiliateCommission_ProPlan() {
            // Given
            BigDecimal bookingAmount = new BigDecimal("100.00");

            // When
            BigDecimal commission = calculatorService.calculateAffiliateCommission(
                    bookingAmount,
                    PRO_AFFILIATE_RATE
            );

            // Then
            assertEquals(new BigDecimal("5.00"), commission);
        }

        @Test
        @DisplayName("Should calculate affiliate commission for ELITE plan (10%)")
        void shouldCalculateAffiliateCommission_ElitePlan() {
            // Given
            BigDecimal bookingAmount = new BigDecimal("100.00");

            // When
            BigDecimal commission = calculatorService.calculateAffiliateCommission(
                    bookingAmount,
                    ELITE_AFFILIATE_RATE
            );

            // Then
            assertEquals(new BigDecimal("10.00"), commission);
        }

        @ParameterizedTest
        @CsvSource({
                "100.00, 0.02, 2.00",
                "100.00, 0.05, 5.00",
                "100.00, 0.10, 10.00",
                "250.00, 0.05, 12.50",
                "1000.00, 0.10, 100.00",
                "33.33, 0.05, 1.67"  // Test rounding (1.6665 -> 1.67)
        })
        @DisplayName("Should calculate affiliate commission correctly for various amounts")
        void shouldCalculateAffiliateCommission_VariousAmounts(
                String amount,
                String rate,
                String expected
        ) {
            // Given
            BigDecimal bookingAmount = new BigDecimal(amount);
            BigDecimal affiliateRate = new BigDecimal(rate);
            BigDecimal expectedCommission = new BigDecimal(expected);

            // When
            BigDecimal commission = calculatorService.calculateAffiliateCommission(
                    bookingAmount,
                    affiliateRate
            );

            // Then
            assertEquals(expectedCommission, commission);
        }

        @Test
        @DisplayName("Should return zero commission for zero booking amount")
        void shouldReturnZero_WhenBookingAmountIsZero() {
            // Given
            BigDecimal bookingAmount = BigDecimal.ZERO;

            // When
            BigDecimal commission = calculatorService.calculateAffiliateCommission(
                    bookingAmount,
                    PRO_AFFILIATE_RATE
            );

            // Then
            assertEquals(BigDecimal.ZERO.setScale(2), commission);
        }

        @Test
        @DisplayName("Should throw exception when booking amount is null")
        void shouldThrowException_WhenBookingAmountIsNull() {
            assertThrows(
                    NullPointerException.class,
                    () -> calculatorService.calculateAffiliateCommission(null, PRO_AFFILIATE_RATE)
            );
        }

        @Test
        @DisplayName("Should throw exception when affiliate rate is null")
        void shouldThrowException_WhenAffiliateRateIsNull() {
            assertThrows(
                    NullPointerException.class,
                    () -> calculatorService.calculateAffiliateCommission(new BigDecimal("100.00"), null)
            );
        }

        @Test
        @DisplayName("Should throw exception when booking amount is negative")
        void shouldThrowException_WhenBookingAmountIsNegative() {
            assertThrows(
                    IllegalArgumentException.class,
                    () -> calculatorService.calculateAffiliateCommission(
                            new BigDecimal("-100.00"),
                            PRO_AFFILIATE_RATE
                    )
            );
        }

        @Test
        @DisplayName("Should throw exception when affiliate rate is negative")
        void shouldThrowException_WhenAffiliateRateIsNegative() {
            assertThrows(
                    IllegalArgumentException.class,
                    () -> calculatorService.calculateAffiliateCommission(
                            new BigDecimal("100.00"),
                            new BigDecimal("-0.05")
                    )
            );
        }

        @Test
        @DisplayName("Should throw exception when affiliate rate is greater than 1")
        void shouldThrowException_WhenAffiliateRateIsGreaterThanOne() {
            assertThrows(
                    IllegalArgumentException.class,
                    () -> calculatorService.calculateAffiliateCommission(
                            new BigDecimal("100.00"),
                            new BigDecimal("1.5")
                    )
            );
        }
    }

    // ============================================================================
    // PLATFORM FEE TESTS (Platform CHARGES to Host)
    // ============================================================================

    @Nested
    @DisplayName("Platform Fee Calculations")
    class PlatformFeeTests {

        @Test
        @DisplayName("Should calculate platform fee for FREE plan host (25%)")
        void shouldCalculatePlatformFee_FreePlan() {
            // Given
            BigDecimal bookingAmount = new BigDecimal("100.00");

            // When
            BigDecimal platformFee = calculatorService.calculatePlatformFee(
                    bookingAmount,
                    FREE_HOST_RATE
            );

            // Then
            assertEquals(new BigDecimal("25.00"), platformFee);
        }

        @Test
        @DisplayName("Should calculate platform fee for PRO plan host (20%)")
        void shouldCalculatePlatformFee_ProPlan() {
            // Given
            BigDecimal bookingAmount = new BigDecimal("100.00");

            // When
            BigDecimal platformFee = calculatorService.calculatePlatformFee(
                    bookingAmount,
                    PRO_HOST_RATE
            );

            // Then
            assertEquals(new BigDecimal("20.00"), platformFee);
        }

        @Test
        @DisplayName("Should calculate platform fee for ELITE plan host (15%)")
        void shouldCalculatePlatformFee_ElitePlan() {
            // Given
            BigDecimal bookingAmount = new BigDecimal("100.00");

            // When
            BigDecimal platformFee = calculatorService.calculatePlatformFee(
                    bookingAmount,
                    ELITE_HOST_RATE
            );

            // Then
            assertEquals(new BigDecimal("15.00"), platformFee);
        }

        @ParameterizedTest
        @CsvSource({
                "100.00, 0.25, 25.00",
                "100.00, 0.20, 20.00",
                "100.00, 0.15, 15.00",
                "500.00, 0.20, 100.00",
                "1000.00, 0.15, 150.00"
        })
        @DisplayName("Should calculate platform fee correctly for various amounts")
        void shouldCalculatePlatformFee_VariousAmounts(
                String amount,
                String rate,
                String expected
        ) {
            // Given
            BigDecimal bookingAmount = new BigDecimal(amount);
            BigDecimal hostRate = new BigDecimal(rate);
            BigDecimal expectedFee = new BigDecimal(expected);

            // When
            BigDecimal platformFee = calculatorService.calculatePlatformFee(
                    bookingAmount,
                    hostRate
            );

            // Then
            assertEquals(expectedFee, platformFee);
        }
    }

    // ============================================================================
    // HOST NET INCOME TESTS
    // ============================================================================

    @Nested
    @DisplayName("Host Net Income Calculations")
    class HostNetIncomeTests {

        @Test
        @DisplayName("Should calculate host net income for PRO plan ($100 - $20 = $80)")
        void shouldCalculateHostNetIncome_ProPlan() {
            // Given
            BigDecimal bookingAmount = new BigDecimal("100.00");

            // When
            BigDecimal hostNetIncome = calculatorService.calculateHostNetIncome(
                    bookingAmount,
                    PRO_HOST_RATE
            );

            // Then
            assertEquals(new BigDecimal("80.00"), hostNetIncome);
        }

        @ParameterizedTest
        @CsvSource({
                "100.00, 0.25, 75.00",   // FREE: $100 - $25 = $75
                "100.00, 0.20, 80.00",   // PRO: $100 - $20 = $80
                "100.00, 0.15, 85.00",   // ELITE: $100 - $15 = $85
                "500.00, 0.20, 400.00",  // PRO: $500 - $100 = $400
        })
        @DisplayName("Should calculate host net income correctly")
        void shouldCalculateHostNetIncome_VariousAmounts(
                String amount,
                String rate,
                String expected
        ) {
            // Given
            BigDecimal bookingAmount = new BigDecimal(amount);
            BigDecimal hostRate = new BigDecimal(rate);
            BigDecimal expectedIncome = new BigDecimal(expected);

            // When
            BigDecimal hostNetIncome = calculatorService.calculateHostNetIncome(
                    bookingAmount,
                    hostRate
            );

            // Then
            assertEquals(expectedIncome, hostNetIncome);
        }
    }

    // ============================================================================
    // PLATFORM PROFIT TESTS
    // ============================================================================

    @Nested
    @DisplayName("Platform Profit Calculations")
    class PlatformProfitTests {

        @Test
        @DisplayName("Should calculate platform profit for PRO plans ($20 - $5 = $15)")
        void shouldCalculatePlatformProfit_ProPlan() {
            // Given
            BigDecimal bookingAmount = new BigDecimal("100.00");

            // When
            BigDecimal platformProfit = calculatorService.calculatePlatformProfit(
                    bookingAmount,
                    PRO_HOST_RATE,      // 20% = $20
                    PRO_AFFILIATE_RATE  // 5% = $5
            );

            // Then
            assertEquals(new BigDecimal("15.00"), platformProfit);
        }

        @ParameterizedTest
        @CsvSource({
                "100.00, 0.25, 0.02, 23.00",  // FREE host, FREE affiliate: $25 - $2 = $23
                "100.00, 0.20, 0.05, 15.00",  // PRO host, PRO affiliate: $20 - $5 = $15
                "100.00, 0.15, 0.10, 5.00",   // ELITE host, ELITE affiliate: $15 - $10 = $5
                "100.00, 0.25, 0.10, 15.00",  // FREE host, ELITE affiliate: $25 - $10 = $15
                "500.00, 0.20, 0.05, 75.00",  // PRO: $100 - $25 = $75
        })
        @DisplayName("Should calculate platform profit correctly for various combinations")
        void shouldCalculatePlatformProfit_VariousCombinations(
                String amount,
                String hostRate,
                String affiliateRate,
                String expected
        ) {
            // Given
            BigDecimal bookingAmount = new BigDecimal(amount);
            BigDecimal hostCommissionRate = new BigDecimal(hostRate);
            BigDecimal affiliateCommissionRate = new BigDecimal(affiliateRate);
            BigDecimal expectedProfit = new BigDecimal(expected);

            // When
            BigDecimal platformProfit = calculatorService.calculatePlatformProfit(
                    bookingAmount,
                    hostCommissionRate,
                    affiliateCommissionRate
            );

            // Then
            assertEquals(expectedProfit, platformProfit);
        }
    }

    // ============================================================================
    // COMMISSION BREAKDOWN TESTS
    // ============================================================================

    @Nested
    @DisplayName("Commission Breakdown Calculations")
    class CommissionBreakdownTests {

        @Test
        @DisplayName("Should calculate complete breakdown for PRO plans ($100 booking)")
        void shouldCalculateCompleteBreakdown_ProPlan() {
            // Given
            BigDecimal bookingAmount = new BigDecimal("100.00");

            // When
            CommissionCalculatorService.CommissionBreakdown breakdown =
                    calculatorService.calculateBreakdown(
                            bookingAmount,
                            PRO_HOST_RATE,
                            PRO_AFFILIATE_RATE
                    );

            // Then
            assertEquals(new BigDecimal("100.00"), breakdown.bookingAmount());
            assertEquals(new BigDecimal("20.00"), breakdown.platformFee());
            assertEquals(new BigDecimal("5.00"), breakdown.affiliateCommission());
            assertEquals(new BigDecimal("80.00"), breakdown.hostNetIncome());
            assertEquals(new BigDecimal("15.00"), breakdown.platformProfit());
        }

        @Test
        @DisplayName("Should calculate complete breakdown for ELITE plans ($250 booking)")
        void shouldCalculateCompleteBreakdown_ElitePlan() {
            // Given
            BigDecimal bookingAmount = new BigDecimal("250.00");

            // When
            CommissionCalculatorService.CommissionBreakdown breakdown =
                    calculatorService.calculateBreakdown(
                            bookingAmount,
                            ELITE_HOST_RATE,
                            ELITE_AFFILIATE_RATE
                    );

            // Then
            assertEquals(new BigDecimal("250.00"), breakdown.bookingAmount());
            assertEquals(new BigDecimal("37.50"), breakdown.platformFee());      // 15% of $250
            assertEquals(new BigDecimal("25.00"), breakdown.affiliateCommission()); // 10% of $250
            assertEquals(new BigDecimal("212.50"), breakdown.hostNetIncome());    // $250 - $37.50
            assertEquals(new BigDecimal("12.50"), breakdown.platformProfit());    // $37.50 - $25
        }

        @Test
        @DisplayName("Should throw exception when breakdown is created with null values")
        void shouldThrowException_WhenBreakdownHasNullValues() {
            assertThrows(
                    NullPointerException.class,
                    () -> new CommissionCalculatorService.CommissionBreakdown(
                            null,
                            BigDecimal.ZERO,
                            BigDecimal.ZERO,
                            BigDecimal.ZERO,
                            BigDecimal.ZERO
                    )
            );
        }
    }

    // ============================================================================
    // EDGE CASES
    // ============================================================================

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCaseTests {

        @Test
        @DisplayName("Should handle very large booking amounts")
        void shouldHandleLargeBookingAmounts() {
            // Given
            BigDecimal bookingAmount = new BigDecimal("1000000.00"); // $1 million

            // When
            CommissionCalculatorService.CommissionBreakdown breakdown =
                    calculatorService.calculateBreakdown(
                            bookingAmount,
                            PRO_HOST_RATE,
                            PRO_AFFILIATE_RATE
                    );

            // Then
            assertEquals(new BigDecimal("200000.00"), breakdown.platformFee());        // 20%
            assertEquals(new BigDecimal("50000.00"), breakdown.affiliateCommission());  // 5%
            assertEquals(new BigDecimal("800000.00"), breakdown.hostNetIncome());
            assertEquals(new BigDecimal("150000.00"), breakdown.platformProfit());
        }

        @Test
        @DisplayName("Should handle very small booking amounts")
        void shouldHandleSmallBookingAmounts() {
            // Given
            BigDecimal bookingAmount = new BigDecimal("1.00");

            // When
            CommissionCalculatorService.CommissionBreakdown breakdown =
                    calculatorService.calculateBreakdown(
                            bookingAmount,
                            PRO_HOST_RATE,
                            PRO_AFFILIATE_RATE
                    );

            // Then
            assertEquals(new BigDecimal("0.20"), breakdown.platformFee());
            assertEquals(new BigDecimal("0.05"), breakdown.affiliateCommission());
            assertEquals(new BigDecimal("0.80"), breakdown.hostNetIncome());
            assertEquals(new BigDecimal("0.15"), breakdown.platformProfit());
        }

        @Test
        @DisplayName("Should round half-up correctly for amounts with many decimals")
        void shouldRoundCorrectly() {
            // Given
            BigDecimal bookingAmount = new BigDecimal("33.33"); // Will produce 1.6665 for 5% = 1.67 (half-up)

            // When
            BigDecimal commission = calculatorService.calculateAffiliateCommission(
                    bookingAmount,
                    PRO_AFFILIATE_RATE
            );

            // Then
            assertEquals(new BigDecimal("1.67"), commission); // Rounded up from 1.6665
        }
    }
}
