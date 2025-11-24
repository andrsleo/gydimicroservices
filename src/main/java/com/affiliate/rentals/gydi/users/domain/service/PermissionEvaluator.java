package com.affiliate.rentals.gydi.users.domain.service;

import com.affiliate.rentals.gydi.users.domain.model.*;

/**
 * Domain service for evaluating user permissions in the GYDI platform.
 *
 * <p>This service implements the core business logic for determining whether
 * a user has permission to perform specific actions. It evaluates permissions
 * based on:
 * </p>
 * <ul>
 *   <li>User role (USER vs ADMIN)</li>
 *   <li>Subscription plan (FREE, PRO, ELITE)</li>
 *   <li>User capabilities (canPublish, canRefer, canRent)</li>
 *   <li>Account verification status</li>
 * </ul>
 *
 * <p>This is a stateless service with only static methods,  following Domain-Driven Design principles.
 * It contains no framework dependencies and represents pure business logic.</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * User user = // ... get user
 * boolean canPublish = PermissionEvaluator.evaluate(user, Permission.PROPERTY_PUBLISH);
 * boolean withinLimit = PermissionEvaluator.canExceedLimit(user, ResourceType.PROPERTY, 5);
 * }</pre>
 *
 * @author GYDI Development Team
 * @see Permission
 * @see User
 * @see SubscriptionPlan
 */
public final class PermissionEvaluator {

    private PermissionEvaluator() {
        // Private constructor to prevent instantiation
        throw new AssertionError("PermissionEvaluator is a utility class and should not be instantiated");
    }

    /**
     * Evaluates whether a user has a specific permission.
     *
     * <p>This method performs a comprehensive check including:
     * <ul>
     *   <li>Admin bypass - admins have all permissions</li>
     *   <li>Verification requirements - some permissions require verified account</li>
     *   <li>Capability checks - permissions may require specific capabilities</li>
     *   <li>Plan-based permissions - some features require paid plans</li>
     * </ul>
     *
     * @param user the user to evaluate
     * @param permission the permission to check
     * @return {@code true} if the user has the permission, {@code false} otherwise
     * @throws NullPointerException if user or permission is {@code null}
     */
    public static boolean evaluate(User user, Permission permission) {
        if (user == null) {
            throw new NullPointerException("User cannot be null");
        }
        if (permission == null) {
            throw new NullPointerException("Permission cannot be null");
        }

        // Admin bypass - admins have all permissions
        if (user.isAdmin()) {
            return true;
        }

        // Verification check - some permissions require verified account
        if (permission.requiresVerification() && !user.isAccountVerified()) {
            return false;
        }

        // Permission-specific evaluation
        return switch (permission) {
            // Profile permissions - available to all authenticated users
            case PROFILE_READ, PROFILE_UPDATE, PROFILE_DELETE -> true;

            // Property permissions - require canPublish capability
            case PROPERTY_PUBLISH -> user.capabilities().canPublish();
            case PROPERTY_UPDATE_OWN, PROPERTY_DELETE_OWN -> true; // Ownership check in use case

            // Rental permissions - require canRent capability
            case PROPERTY_RENT -> user.capabilities().canRent();

            // Referral permissions - require canRefer capability
            case REFERRAL_GENERATE -> user.capabilities().canRefer();
            case REFERRAL_VIEW_OWN -> user.capabilities().canRefer();
            
            // Analytics permissions - plan-based
            case ANALYTICS_VIEW_OWN -> true;
            case ANALYTICS_VIEW_ADVANCED ->
                user.activePlan() == SubscriptionPlan.PRO ||
                user.activePlan() == SubscriptionPlan.ELITE;
            case ANALYTICS_EXPORT ->
                user.activePlan() == SubscriptionPlan.ELITE;

            // Admin-only permissions
            case PROPERTY_VIEW_ANY, PROPERTY_UPDATE_ANY, PROPERTY_DELETE_ANY, PROPERTY_MODERATE,
                 REFERRAL_VIEW_ANY, ANALYTICS_VIEW_GLOBAL,
                 USER_MANAGE, SUBSCRIPTION_MANAGE, SYSTEM_CONFIGURE,
                 MODERATION_MANAGE, PAYMENT_VIEW, REPORT_GENERATE -> false;
        };
    }

    /**
     * Checks if a user can create additional resources without exceeding their plan limit.
     *
     * <p>This method evaluates whether a user has reached their subscription plan's
     * limit for a specific resource type. Admins always have unlimited resources.</p>
     *
     * @param user the user to evaluate
     * @param resourceType the type of resource (PROPERTY or REFERRAL)
     * @param currentCount the user's current count of this resource type
     * @return {@code true} if the user can create more resources, {@code false} if limit reached
     * @throws NullPointerException if any parameter is {@code null}
     */
    public static boolean canExceedLimit(User user, ResourceType resourceType, int currentCount) {
        if (user == null) {
            throw new NullPointerException("User cannot be null");
        }
        if (resourceType == null) {
            throw new NullPointerException("ResourceType cannot be null");
        }
        if (currentCount < 0) {
            throw new IllegalArgumentException("Current count cannot be negative");
        }

        // Admin bypass - admins have unlimited resources
        if (user.isAdmin()) {
            return true;
        }

        int limit = switch (resourceType) {
            case PROPERTY -> user.activePlan().getPropertyPublishLimit();
            case REFERRAL -> user.activePlan().getReferralGenerateLimit();
        };

        return currentCount < limit;
    }

    /**
     * Gets the remaining resources a user can create before hitting their limit.
     *
     * @param user the user to evaluate
     * @param resourceType the type of resource
     * @param currentCount the user's current count of this resource type
     * @return the number of remaining resources, or {@link Integer#MAX_VALUE} for admins/unlimited plans
     * @throws NullPointerException if user or resourceType is {@code null}
     * @throws IllegalArgumentException if currentCount is negative
     */
    public static int getRemainingResources(User user, ResourceType resourceType, int currentCount) {
        if (user == null) {
            throw new NullPointerException("User cannot be null");
        }
        if (resourceType == null) {
            throw new NullPointerException("ResourceType cannot be null");
        }
        if (currentCount < 0) {
            throw new IllegalArgumentException("Current count cannot be negative");
        }

        // Admin or unlimited plan
        if (user.isAdmin()) {
            return Integer.MAX_VALUE;
        }

        int limit = switch (resourceType) {
            case PROPERTY -> user.activePlan().getPropertyPublishLimit();
            case REFERRAL -> user.activePlan().getReferralGenerateLimit();
        };

        // Unlimited plan
        if (limit == Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }

        return Math.max(0, limit - currentCount);
    }

    /**
     * Checks if a user has all specified permissions.
     *
     * @param user the user to evaluate
     * @param permissions the permissions to check
     * @return {@code true} if the user has ALL permissions, {@code false} otherwise
     * @throws NullPointerException if user or permissions is {@code null}
     */
    public static boolean hasAllPermissions(User user, Permission... permissions) {
        if (user == null) {
            throw new NullPointerException("User cannot be null");
        }
        if (permissions == null) {
            throw new NullPointerException("Permissions cannot be null");
        }

        for (Permission permission : permissions) {
            if (!evaluate(user, permission)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Checks if a user has at least one of the specified permissions.
     *
     * @param user the user to evaluate
     * @param permissions the permissions to check
     * @return {@code true} if the user has AT LEAST ONE permission, {@code false} otherwise
     * @throws NullPointerException if user or permissions is {@code null}
     */
    public static boolean hasAnyPermission(User user, Permission... permissions) {
        if (user == null) {
            throw new NullPointerException("User cannot be null");
        }
        if (permissions == null) {
            throw new NullPointerException("Permissions cannot be null");
        }

        for (Permission permission : permissions) {
            if (evaluate(user, permission)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Gets a descriptive message for why a permission was denied.
     *
     * @param user the user being evaluated
     * @param permission the permission that was denied
     * @return a human-readable message explaining why the permission was denied
     * @throws NullPointerException if user or permission is {@code null}
     */
    public static String getDenialReason(User user, Permission permission) {
        if (user == null) {
            throw new NullPointerException("User cannot be null");
        }
        if (permission == null) {
            throw new NullPointerException("Permission cannot be null");
        }

        // If user actually has the permission, no denial reason
        if (evaluate(user, permission)) {
            return "Permission granted";
        }

        // Check verification
        if (permission.requiresVerification() && !user.isAccountVerified()) {
            return "Account verification required for this action";
        }

        // Check capabilities
        if (permission == Permission.PROPERTY_PUBLISH && !user.capabilities().canPublish()) {
            return "Property publishing capability is disabled for your account";
        }
        if (permission == Permission.REFERRAL_GENERATE && !user.capabilities().canRefer()) {
            return "Referral generation capability is disabled for your account";
        }
        if ((permission == Permission.PROPERTY_RENT) &&
            !user.capabilities().canRent()) {
            return "Property rental capability is disabled for your account";
        }

        // Check plan requirements
        if (permission == Permission.ANALYTICS_VIEW_ADVANCED) {
            return "Advanced analytics requires PRO or ELITE plan. Current plan: " + user.activePlan();
        }
        if (permission == Permission.ANALYTICS_EXPORT) {
            return "Analytics export requires ELITE plan. Current plan: " + user.activePlan();
        }

        // Admin-only
        if (permission.isAdminOnly()) {
            return "This action is restricted to administrators";
        }

        return "Permission denied";
    }
}