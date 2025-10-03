package com.affiliate.rentals.gydi.users.domain.model;

/**
 * Enumeration of role names in the rental system.
 *
 * <p>This enum provides type safety and compile-time validation for role names.
 * It supports three primary roles:</p>
 * <ul>
 *   <li>{@code OWNER} - Property owners who can list and manage rental properties</li>
 *   <li>{@code GUEST} - Users who can book and review properties</li>
 *   <li>{@code ADMIN} - System administrators with elevated privileges</li>
 * </ul>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * RoleName roleName = RoleName.OWNER;
 * String value = roleName.getValue();
 * RoleName parsed = RoleName.fromValue("GUEST");
 * }</pre>
 *
 * @author GYDI Development Team
 * @see Role
 */
public enum RoleName {
    /**
     * Role for property owners who can list and manage rental properties.
     */
    OWNER("OWNER"),

    /**
     * Role for guests who can book and review properties.
     */
    GUEST("GUEST"),

    /**
     * Role for system administrators with elevated privileges.
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
