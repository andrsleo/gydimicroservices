package com.affiliate.rentals.gydi.users.domain.model;

/**
 * Enumeration representing profile visibility levels.
 *
 * <p>This enum defines who can view a user's profile information,
 * supporting privacy controls and GDPR compliance.</p>
 *
 * @author GYDI Development Team
 */
public enum ProfileVisibility {
    /**
     * Profile is visible to everyone.
     */
    PUBLIC,

    /**
     * Profile is visible only to connections/contacts.
     */
    CONNECTIONS,

    /**
     * Profile is private and only visible to the owner.
     */
    PRIVATE;

    /**
     * Converts a string value to ProfileVisibility enum.
     *
     * @param value the string value
     * @return the corresponding ProfileVisibility enum, or PUBLIC as default
     */
    public static ProfileVisibility fromString(String value) {
        if (value == null) {
            return PUBLIC;
        }
        try {
            return ProfileVisibility.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return PUBLIC;
        }
    }

    /**
     * Returns the lowercase string representation.
     *
     * @return lowercase visibility string
     */
    public String toValue() {
        return name().toLowerCase();
    }
}