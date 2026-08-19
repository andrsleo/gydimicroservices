package com.affiliate.rentals.gydi.subscriptions.application.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

/**
 * Response DTO representing a complete commission breakdown for a booking transaction.
 *
 * <p><strong>Business Model:</strong></p>
 * <ul>
 *   <li><strong>Platform Fee</strong>: Amount platform CHARGES to host</li>
 *   <li><strong>Affiliate Commission</strong>: Amount platform PAYS to affiliate</li>
 *   <li><strong>Host Net Income</strong>: Amount host receives (booking - platform fee)</li>
 *   <li><strong>Platform Profit</strong>: Amount platform keeps (platform fee - affiliate commission)</li>
 * </ul>
 *
 * <p><strong>Example (PRO plan, $100 booking):</strong></p>
 * <pre>
 * {
 *   "bookingAmount": 100.00,
 *   "platformFee": 20.00,          // Platform CHARGES host 20%
 *   "affiliateCommission": 5.00,    // Platform PAYS affiliate 5%
 *   "hostNetIncome": 80.00,         // Host receives $80
 *   "platformProfit": 15.00,        // Platform keeps $15
 *   "hostPlan": "PRO",
 *   "affiliatePlan": "PRO"
 * }
 * </pre>
 *
 * @param bookingAmount Total booking amount
 * @param platformFee Amount platform charges to host
 * @param affiliateCommission Amount platform pays to affiliate
 * @param hostNetIncome Amount host receives after platform fee
 * @param platformProfit Platform's net profit (platformFee - affiliateCommission)
 * @param hostPlan Host's subscription plan code
 * @param affiliatePlan Affiliate's subscription plan code
 *
 * @author GYDI Development Team
 * @version 2.0
 * @since 2026-02-04
 */
public record CommissionBreakdownResponse(

        @JsonProperty("booking_amount")
        BigDecimal bookingAmount,

        @JsonProperty("platform_fee")
        BigDecimal platformFee,

        @JsonProperty("affiliate_commission")
        BigDecimal affiliateCommission,

        @JsonProperty("host_net_income")
        BigDecimal hostNetIncome,

        @JsonProperty("platform_profit")
        BigDecimal platformProfit,

        @JsonProperty("host_plan")
        String hostPlan,

        @JsonProperty("affiliate_plan")
        String affiliatePlan

) {
    /**
     * Factory method to create response from domain calculation and plan codes.
     *
     * @param bookingAmount Total booking amount
     * @param platformFee Platform fee charged to host
     * @param affiliateCommission Commission paid to affiliate
     * @param hostNetIncome Host's net income
     * @param platformProfit Platform's profit
     * @param hostPlan Host's plan code
     * @param affiliatePlan Affiliate's plan code
     * @return CommissionBreakdownResponse instance
     */
    public static CommissionBreakdownResponse of(
            BigDecimal bookingAmount,
            BigDecimal platformFee,
            BigDecimal affiliateCommission,
            BigDecimal hostNetIncome,
            BigDecimal platformProfit,
            String hostPlan,
            String affiliatePlan
    ) {
        return new CommissionBreakdownResponse(
                bookingAmount,
                platformFee,
                affiliateCommission,
                hostNetIncome,
                platformProfit,
                hostPlan,
                affiliatePlan
        );
    }
}
