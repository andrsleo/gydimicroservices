package com.affiliate.rentals.gydi.users.domain.model;

/**
 * Enumeration of role names in the GYDI platform.
 *
 * <p>This enum provides type safety and compile-time validation for role names.
 * It supports two primary roles:</p>
 * <ul>
 *   <li>{@code USER} - Base unified role for all authenticated users (property owners, referrers, renters)</li>
 *   <li>{@code ADMIN} - System administrators with full access and elevated privileges</li>
 * </ul>
 *
 * <p>Note: The USER role is unified and combines capabilities of property owners,
 * referrers, and renters. Access to specific features is determined by:</p>
 * <ul>
 *   <li>Subscription plan (FREE, PRO, ELITE)</li>
 *   <li>User capabilities (canPublish, canRefer, canRent)</li>
 *   <li>Account verification status</li>
 * </ul>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * RoleName roleName = RoleName.USER;
 * String value = roleName.getValue();
 * RoleName parsed = RoleName.fromValue("ADMIN");
 * }</pre>
 *
 * @author GYDI Development Team
 * @see Role
 * @see SubscriptionPlan
 * @see UserCapabilities
 */
public enum RoleName {
    /**
     * Unified role for all regular users of the platform.
     * Capabilities are determined by subscription plan and user capabilities.
     */
    USER("USER"),

    /**
     * Role for system administrators with complete access.
     * Admins bypass all subscription limits and capability checks.
     */
    ADMIN("ADMIN");

    private final String value;

    /**
     * Constructs a RoleName with the specified string value.
     *
     * @param value the string representation of this role name
     */
    RoleName(String value) {
        this.value = value;
    }

    /**
     * Returns the string value of this role name.
     *
     * @return the role name as a string
     */
    public String getValue() {
        return value;
    }

    /**
     * Converts a string value to its corresponding RoleName enum constant.
     * The comparison is case-insensitive.
     *
     * @param value the string value to convert
     * @return the matching RoleName
     * @throws IllegalArgumentException if the value doesn't match any role name
     */
    public static RoleName fromValue(String value) {
        for (RoleName roleName : RoleName.values()) {
            if (roleName.value.equalsIgnoreCase(value)) {
                return roleName;
            }
        }
        throw new IllegalArgumentException("Invalid role name: " + value);
    }
}
