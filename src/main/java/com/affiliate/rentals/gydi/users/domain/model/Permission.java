package com.affiliate.rentals.gydi.users.domain.model;

/**
 * Enumeration of permissions in the GYDI platform.
 *
 * <p>This enum defines fine-grained permissions that control access to specific
 * operations within the system. Permissions are evaluated based on user roles,
 * subscription plans, capabilities, and verification status.</p>
 *
 * <p>Permissions are categorized by domain:</p>
 * <ul>
 *   <li><strong>Profile</strong> - User profile management</li>
 *   <li><strong>Property</strong> - Property CRUD and moderation</li>
 *   <li><strong>Referral</strong> - Referral link generation and tracking</li>
 *   <li><strong>Rental</strong> - Property booking and rental</li>
 *   <li><strong>Analytics</strong> - Statistics and reporting</li>
 *   <li><strong>Admin</strong> - System administration</li>
 * </ul>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * Permission permission = Permission.PROPERTY_PUBLISH;
 * boolean requiresVerification = permission.requiresVerification();
 * boolean isAdminOnly = permission.isAdminOnly();
 * }</pre>
 *
 * @author GYDI Development Team
 */
public enum Permission {
    // Profile permissions
    /**
     * Permission to read own profile information.
     * Available to all authenticated users.
     */
    PROFILE_READ,

    /**
     * Permission to update own profile information.
     * Available to all authenticated users.
     */
    PROFILE_UPDATE,

    /**
     * Permission to delete own profile.
     * Available to all authenticated users.
     */
    PROFILE_DELETE,

    // Property permissions
    /**
     * Permission to publish properties.
     * Requires: verified account, canPublish capability.
     * Subject to plan limits.
     */
    PROPERTY_PUBLISH,

    /**
     * Permission to update own properties.
     * Available to property owners.
     */
    PROPERTY_UPDATE_OWN,

    /**
     * Permission to delete own properties.
     * Available to property owners.
     */
    PROPERTY_DELETE_OWN,

    /**
     * Permission to view any property (including non-public).
     * Admin-only permission.
     */
    PROPERTY_VIEW_ANY,

    /**
     * Permission to update any property.
     * Admin-only permission.
     */
    PROPERTY_UPDATE_ANY,

    /**
     * Permission to delete any property.
     * Admin-only permission.
     */
    PROPERTY_DELETE_ANY,

    /**
     * Permission to moderate properties (approve, reject, flag).
     * Admin-only permission.
     */
    PROPERTY_MODERATE,

    // Referral permissions
    /**
     * Permission to generate referral links.
     * Requires: verified account, canRefer capability.
     * Subject to plan limits.
     */
    REFERRAL_GENERATE,

    /**
     * Permission to view own referral statistics.
     * Available to all users who can refer.
     */
    REFERRAL_VIEW_OWN,

    /**
     * Permission to view any user's referral statistics.
     * Admin-only permission.
     */
    REFERRAL_VIEW_ANY,

    // Rental permissions
    /**
     * Permission to rent properties.
     * Requires: canRent capability.
     */
    PROPERTY_RENT,

    /**
     * Permission to create a booking.
     * Requires: canRent capability.
     */
    BOOKING_CREATE,

    /**
     * Permission to view own bookings.
     * Available to all users.
     */
    BOOKING_VIEW_OWN,

    /**
     * Permission to view any booking.
     * Admin-only permission.
     */
    BOOKING_VIEW_ANY,

    // Analytics permissions
    /**
     * Permission to view own analytics and statistics.
     * Available to all users.
     */
    ANALYTICS_VIEW_OWN,

    /**
     * Permission to view advanced analytics.
     * Requires: PRO or ELITE plan.
     */
    ANALYTICS_VIEW_ADVANCED,

    /**
     * Permission to export analytics data.
     * Requires: ELITE plan.
     */
    ANALYTICS_EXPORT,

    /**
     * Permission to view global platform analytics.
     * Admin-only permission.
     */
    ANALYTICS_VIEW_GLOBAL,

    // Admin permissions
    /**
     * Permission to manage users (create, update, delete, suspend).
     * Admin-only permission.
     */
    USER_MANAGE,

    /**
     * Permission to manage subscription plans.
     * Admin-only permission.
     */
    SUBSCRIPTION_MANAGE,

    /**
     * Permission to configure system settings.
     * Admin-only permission.
     */
    SYSTEM_CONFIGURE,

    /**
     * Permission to manage content moderation.
     * Admin-only permission.
     */
    MODERATION_MANAGE,

    /**
     * Permission to view payment information.
     * Admin-only permission.
     */
    PAYMENT_VIEW,

    /**
     * Permission to generate system reports.
     * Admin-only permission.
     */
    REPORT_GENERATE;

    /**
     * Checks if this permission is restricted to admin users only.
     *
     * @return {@code true} if this is an admin-only permission, {@code false} otherwise
     */
    public boolean isAdminOnly() {
        return switch (this) {
            case PROPERTY_UPDATE_ANY,
                 PROPERTY_DELETE_ANY,
                 PROPERTY_VIEW_ANY,
                 PROPERTY_MODERATE,
                 REFERRAL_VIEW_ANY,
                 BOOKING_VIEW_ANY,
                 ANALYTICS_VIEW_GLOBAL,
                 USER_MANAGE,
                 SUBSCRIPTION_MANAGE,
                 SYSTEM_CONFIGURE,
                 MODERATION_MANAGE,
                 PAYMENT_VIEW,
                 REPORT_GENERATE -> true;
            default -> false;
        };
    }

    /**
     * Checks if this permission requires account verification.
     *
     * @return {@code true} if verification is required, {@code false} otherwise
     */
    public boolean requiresVerification() {
        return switch (this) {
            case PROPERTY_PUBLISH, REFERRAL_GENERATE -> true;
            default -> false;
        };
    }

    /**
     * Checks if this permission requires a specific capability flag.
     *
     * @return {@code true} if a capability check is needed, {@code false} otherwise
     */
    public boolean requiresCapabilityCheck() {
        return switch (this) {
            case PROPERTY_PUBLISH, REFERRAL_GENERATE, PROPERTY_RENT, BOOKING_CREATE -> true;
            default -> false;
        };
    }

    /**
     * Checks if this permission is subject to plan-based limits.
     *
     * @return {@code true} if plan limits apply, {@code false} otherwise
     */
    public boolean hasResourceLimits() {
        return switch (this) {
            case PROPERTY_PUBLISH, REFERRAL_GENERATE -> true;
            default -> false;
        };
    }

    /**
     * Checks if this permission requires a minimum subscription plan.
     *
     * @return {@code true} if a plan upgrade might be needed, {@code false} otherwise
     */
    public boolean requiresPaidPlan() {
        return switch (this) {
            case ANALYTICS_VIEW_ADVANCED, ANALYTICS_EXPORT -> true;
            default -> false;
        };
    }

    /**
     * Returns a human-readable description of this permission.
     *
     * @return a formatted string describing the permission
     */
    public String getDescription() {
        return switch (this) {
            case PROFILE_READ -> "Read own profile";
            case PROFILE_UPDATE -> "Update own profile";
            case PROFILE_DELETE -> "Delete own profile";
            case PROPERTY_PUBLISH -> "Publish properties";
            case PROPERTY_UPDATE_OWN -> "Update own properties";
            case PROPERTY_DELETE_OWN -> "Delete own properties";
            case PROPERTY_VIEW_ANY -> "View any property";
            case PROPERTY_UPDATE_ANY -> "Update any property";
            case PROPERTY_DELETE_ANY -> "Delete any property";
            case PROPERTY_MODERATE -> "Moderate properties";
            case REFERRAL_GENERATE -> "Generate referral links";
            case REFERRAL_VIEW_OWN -> "View own referrals";
            case REFERRAL_VIEW_ANY -> "View any referrals";
            case PROPERTY_RENT -> "Rent properties";
            case BOOKING_CREATE -> "Create bookings";
            case BOOKING_VIEW_OWN -> "View own bookings";
            case BOOKING_VIEW_ANY -> "View any bookings";
            case ANALYTICS_VIEW_OWN -> "View own analytics";
            case ANALYTICS_VIEW_ADVANCED -> "View advanced analytics";
            case ANALYTICS_EXPORT -> "Export analytics";
            case ANALYTICS_VIEW_GLOBAL -> "View global analytics";
            case USER_MANAGE -> "Manage users";
            case SUBSCRIPTION_MANAGE -> "Manage subscriptions";
            case SYSTEM_CONFIGURE -> "Configure system";
            case MODERATION_MANAGE -> "Manage moderation";
            case PAYMENT_VIEW -> "View payments";
            case REPORT_GENERATE -> "Generate reports";
        };
    }
}