package com.affiliate.rentals.gydi.subscriptions.application.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO for subscription plan responses.
 *
 * <p>This record encapsulates subscription plan information returned to clients.
 * It includes pricing, commission rates, and feature limits.</p>
 *
 * @param id the unique identifier of the plan
 * @param planCode the unique code (e.g., "FREE", "PRO", "ELITE")
 * @param planName the display name of the plan
 * @param description the plan description
 * @param monthlyPrice the monthly subscription price
 * @param currency the currency code (e.g., "USD")
 * @param commissionRate the commission rate for referrals (as decimal)
 * @param referralLimitPerMonth maximum referrals per month
 * @param propertyPublishLimit maximum properties that can be published
 * @param isActive whether the plan is active and available
 * @param isFeatured whether the plan should be highlighted
 * @param displayOrder the display order for UI sorting
 * @param createdAt timestamp when the plan was created
 * @param updatedAt timestamp when the plan was last updated
 *
 * @author GYDI Development Team
 */
public record PlanResponse(
        Long id,
        String planCode,
        String planName,
        String description,
        BigDecimal monthlyPrice,
        String currency,
        BigDecimal commissionRate,
        Integer referralLimitPerMonth,
        Integer propertyPublishLimit,
        Boolean isActive,
        Boolean isFeatured,
        Integer displayOrder,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
