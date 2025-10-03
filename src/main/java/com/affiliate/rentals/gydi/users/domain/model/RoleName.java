package com.affiliate.rentals.gydi.users.domain.model;

/**
 * Role names enumeration.
 * Using enum for type safety and compile-time validation.
 */
public enum RoleName {
    OWNER("OWNER"),
    GUEST("GUEST"),
    ADMIN("ADMIN");

    private final String value;

    RoleName(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static RoleName fromValue(String value) {
        for (RoleName roleName : RoleName.values()) {
            if (roleName.value.equalsIgnoreCase(value)) {
                return roleName;
            }
        }
        throw new IllegalArgumentException("Invalid role name: " + value);
    }
}
