package com.affiliate.rentals.gydi.properties.domain.model;

/**
 * Enumeration of media types for property media content.
 *
 * <p>This enum provides type safety and compile-time validation for media types.
 * It supports two primary types of media that can be associated with properties:</p>
 * <ul>
 *   <li>{@code IMAGE} - Static images (photos, pictures)</li>
 *   <li>{@code VIDEO} - Video content (virtual tours, promotional videos)</li>
 * </ul>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * MediaType type = MediaType.IMAGE;
 * String value = type.getValue();
 * MediaType parsed = MediaType.fromValue("VIDEO");
 * }</pre>
 *
 * @author GYDI Development Team
 * @see PropertyMedia
 */
public enum MediaType {
    /**
     * Media type for static images and photos.
     */
    IMAGE("IMAGE"),

    /**
     * Media type for video content.
     */
    VIDEO("VIDEO");

    private final String value;

    /**
     * Constructs a MediaType with the specified string value.
     *
     * @param value the string representation of this media type
     */
    MediaType(String value) {
        this.value = value;
    }

    /**
     * Returns the string value of this media type.
     *
     * @return the media type as a string
     */
    public String getValue() {
        return value;
    }

    /**
     * Converts a string value to its corresponding MediaType enum constant.
     * The comparison is case-insensitive.
     *
     * @param value the string value to convert
     * @return the matching MediaType
     * @throws IllegalArgumentException if the value doesn't match any media type
     */
    public static MediaType fromValue(String value) {
        for (MediaType type : MediaType.values()) {
            if (type.value.equalsIgnoreCase(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Invalid media type: " + value);
    }
}
