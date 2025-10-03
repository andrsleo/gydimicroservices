package com.affiliate.rentals.gydi.properties.domain.model;

/**
 * Media type enumeration.
 * Using enum for type safety and compile-time validation.
 */
public enum MediaType {
    IMAGE("IMAGE"),
    VIDEO("VIDEO");

    private final String value;

    MediaType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static MediaType fromValue(String value) {
        for (MediaType type : MediaType.values()) {
            if (type.value.equalsIgnoreCase(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Invalid media type: " + value);
    }
}
