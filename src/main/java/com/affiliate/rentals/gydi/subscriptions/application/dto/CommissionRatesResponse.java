package com.affiliate.rentals.gydi.subscriptions.application.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Response DTO containing commission rates for all subscription plans.
 *
 * <p>Provides a complete view of commission rates for both affiliates and hosts
 * across all available subscription tiers.</p>
 *
 * <p><strong>Example JSON:</strong></p>
 * <pre>
 * {
 *   "affiliate_rates": {
 *     "FREE": { "plan": "FREE", "earns_percentage": 0.02, "display": "2%" },
 *     "PRO": { "plan": "PRO", "earns_percentage": 0.05, "display": "5%" },
 *     "ELITE": { "plan": "ELITE", "earns_percentage": 0.10, "display": "10%" }
 *   },
 *   "host_rates": {
 *     "FREE": { "plan": "FREE", "platform_charges_percentage": 0.25, "display": "25%" },
 *     "PRO": { "plan": "PRO", "platform_charges_percentage": 0.20, "display": "20%" },
 *     "ELITE": { "plan": "ELITE", "platform_charges_percentage": 0.15, "display": "15%" }
 *   }
 * }
 * </pre>
 *
 * @param affiliateRates Map of plan code to affiliate commission rates
 * @param hostRates Map of plan code to host commission rates
 *
 * @author GYDI Development Team
 * @version 2.0
 * @since 2026-02-04
 */
public record CommissionRatesResponse(

        @JsonProperty("affiliate_rates")
        Map<String, AffiliateRateInfo> affiliateRates,

        @JsonProperty("host_rates")
        Map<String, HostRateInfo> hostRates

) {
    /**
     * Information about affiliate commission rates for a specific plan.
     *
     * @param plan Plan code (FREE, PRO, ELITE)
     * @param earnsPercentage Percentage affiliate earns (platform PAYS this to affiliate)
     * @param display Human-readable percentage (e.g., "5%")
     */
    public record AffiliateRateInfo(
            @JsonProperty("plan")
            String plan,

            @JsonProperty("earns_percentage")
            BigDecimal earnsPercentage,

            @JsonProperty("display")
            String display
    ) {
        /**
         * Factory method to create AffiliateRateInfo from rate.
         *
         * @param plan Plan code
         * @param rate Commission rate (e.g., 0.05 for 5%)
         * @return AffiliateRateInfo instance
         */
        public static AffiliateRateInfo of(String plan, BigDecimal rate) {
            String display = rate.multiply(new BigDecimal("100")).stripTrailingZeros().toPlainString() + "%";
            return new AffiliateRateInfo(plan, rate, display);
        }
    }

    /**
     * Information about host commission rates (platform fees) for a specific plan.
     *
     * @param plan Plan code (FREE, PRO, ELITE)
     * @param platformChargesPercentage Percentage platform charges to host (platform fee)
     * @param display Human-readable percentage (e.g., "20%")
     */
    public record HostRateInfo(
            @JsonProperty("plan")
            String plan,

            @JsonProperty("platform_charges_percentage")
            BigDecimal platformChargesPercentage,

            @JsonProperty("display")
            String display
    ) {
        /**
         * Factory method to create HostRateInfo from rate.
         *
         * @param plan Plan code
         * @param rate Commission rate (e.g., 0.20 for 20%)
         * @return HostRateInfo instance
         */
        public static HostRateInfo of(String plan, BigDecimal rate) {
            String display = rate.multiply(new BigDecimal("100")).stripTrailingZeros().toPlainString() + "%";
            return new HostRateInfo(plan, rate, display);
        }
    }
}
