package com.affiliate.rentals.gydi.users.domain.model;

/**
 * Enumeration representing user gender options.
 *
 * <p>This enum provides gender options following modern best practices
 * for inclusivity and user privacy. It supports standard gender identities
 * while respecting user preferences for privacy.</p>
 *
 * @author GYDI Development Team
 */
public enum Gender {
    /**
     * Male gender identity.
     */
    MALE,

    /**
     * Female gender identity.
     */
    FEMALE,

    /**
     * Non-binary gender identity.
     */
    NON_BINARY,

    /**
     * User prefers not to disclose their gender.
     */
    PREFER_NOT_TO_SAY,

    /**
     * Other gender identity not covered by standard options.
     */
    OTHER;

    /**
     * Converts a string value to Gender enum.
     *
     * @param value the string value
     * @return the corresponding Gender enum, or null if invalid
     */
    public static Gender fromString(String value) {
        if (value == null) {
            return null;
        }
        try {
            return Gender.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Returns the lowercase string representation.
     *
     * @return lowercase gender string
     */
    public String toValue() {
        return name().toLowerCase();
    }
}