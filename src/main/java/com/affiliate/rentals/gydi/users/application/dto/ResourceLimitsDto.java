package com.affiliate.rentals.gydi.users.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Resource Limits DTO
 *
 * Contains information about user's resource usage and limits
 * based on their subscription plan.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResourceLimitsDto {

    private String userId;
    private String subscriptionPlan; // Plan code: "FREE", "PRO", "ELITE"
    private LimitsDto limits;
    private UsageDto usage;

    /**
     * Plan limits
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LimitsDto {
        private Integer properties;
        private Integer referrals;
    }

    /**
     * Current usage
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UsageDto {
        private Integer properties;
        private Integer referrals;
    }
}
