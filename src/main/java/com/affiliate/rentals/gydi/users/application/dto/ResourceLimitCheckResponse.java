package com.affiliate.rentals.gydi.users.application.dto;

/**
 * Response for resource limit check operations.
 *
 * <p>This record encapsulates the result of a resource limit evaluation,
 * indicating whether the user can create more resources of a specific type
 * and providing information about current usage and limits.</p>
 *
 * @param userId the unique identifier of the user
 * @param resourceType the type of resource checked
 * @param currentCount the current count of resources
 * @param limit the maximum allowed resources
 * @param canExceedLimit whether the user can create more resources
 * @param subscriptionPlan the user's current subscription plan
 * @author GYDI Development Team
 */
public record ResourceLimitCheckResponse(
        Long userId,
        String resourceType,
        int currentCount,
        int limit,
        boolean canExceedLimit,
        String subscriptionPlan
) {
    /**
     * Creates a response indicating the limit has not been reached.
     *
     * @param userId the user ID
     * @param resourceType the resource type
     * @param currentCount the current count
     * @param limit the maximum limit
     * @param subscriptionPlan the subscription plan
     * @return a response with canExceedLimit=true
     */
    public static ResourceLimitCheckResponse withinLimit(
            Long userId,
            String resourceType,
            int currentCount,
            int limit,
            String subscriptionPlan) {
        return new ResourceLimitCheckResponse(
                userId,
                resourceType,
                currentCount,
                limit,
                true,
                subscriptionPlan
        );
    }

    /**
     * Creates a response indicating the limit has been reached.
     *
     * @param userId the user ID
     * @param resourceType the resource type
     * @param currentCount the current count
     * @param limit the maximum limit
     * @param subscriptionPlan the subscription plan
     * @return a response with canExceedLimit=false
     */
    public static ResourceLimitCheckResponse limitReached(
            Long userId,
            String resourceType,
            int currentCount,
            int limit,
            String subscriptionPlan) {
        return new ResourceLimitCheckResponse(
                userId,
                resourceType,
                currentCount,
                limit,
                false,
                subscriptionPlan
        );
    }

    /**
     * Calculates the remaining capacity.
     *
     * @return the number of additional resources that can be created
     */
    public int remainingCapacity() {
        return canExceedLimit ? Math.max(0, limit - currentCount) : 0;
    }
}