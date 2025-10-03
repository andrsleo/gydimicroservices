package com.affiliate.rentals.gydi.properties.domain.model;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * PropertyMedia value object representing media content associated with a rental property.
 *
 * <p>This is an immutable value object following Domain-Driven Design (DDD) principles.
 * It encapsulates media files (images or videos) that showcase a property. Each media
 * item has a URL, type classification, and creation timestamp for tracking purposes.</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * PropertyMedia photo = PropertyMedia.image("https://example.com/photo.jpg");
 * PropertyMedia video = PropertyMedia.video("https://example.com/tour.mp4");
 * boolean isImage = photo.isImage();
 * }</pre>
 *
 * @param id the media's unique identifier, may be {@code null} for transient media
 * @param mediaUrl the URL to the media file, never {@code null} or blank
 * @param mediaType the type of media (IMAGE or VIDEO), never {@code null}
 * @param createdAt the timestamp when the media was created, never {@code null}
 * @author GYDI Development Team
 * @see MediaType
 */
public record PropertyMedia(
        Long id,
        String mediaUrl,
        MediaType mediaType,
        LocalDateTime createdAt
) {

    /**
     * Compact constructor that validates the media properties.
     *
     * @throws NullPointerException if mediaUrl or mediaType is {@code null}
     * @throws IllegalArgumentException if mediaUrl is blank
     */
    public PropertyMedia {
        Objects.requireNonNull(mediaUrl, "Media URL cannot be null");
        Objects.requireNonNull(mediaType, "Media type cannot be null");
        if (mediaUrl.isBlank()) {
            throw new IllegalArgumentException("Media URL cannot be blank");
        }
        createdAt = Objects.requireNonNullElseGet(createdAt, LocalDateTime::now);
    }

    /**
     * Factory method to create PropertyMedia with all attributes specified.
     *
     * @param id the media ID
     * @param mediaUrl the URL to the media file
     * @param mediaType the type of media
     * @param createdAt the creation timestamp
     * @return a new PropertyMedia instance
     * @throws NullPointerException if mediaUrl or mediaType is {@code null}
     * @throws IllegalArgumentException if mediaUrl is blank
     */
    public static PropertyMedia of(Long id, String mediaUrl, MediaType mediaType, LocalDateTime createdAt) {
        return new PropertyMedia(id, mediaUrl, mediaType, createdAt);
    }

    /**
     * Factory method to create an image PropertyMedia without an ID.
     * The creation timestamp is set to the current time.
     *
     * @param mediaUrl the URL to the image file
     * @return a new PropertyMedia instance of type IMAGE
     * @throws NullPointerException if mediaUrl is {@code null}
     * @throws IllegalArgumentException if mediaUrl is blank
     */
    public static PropertyMedia image(String mediaUrl) {
        return new PropertyMedia(null, mediaUrl, MediaType.IMAGE, LocalDateTime.now());
    }

    /**
     * Factory method to create a video PropertyMedia without an ID.
     * The creation timestamp is set to the current time.
     *
     * @param mediaUrl the URL to the video file
     * @return a new PropertyMedia instance of type VIDEO
     * @throws NullPointerException if mediaUrl is {@code null}
     * @throws IllegalArgumentException if mediaUrl is blank
     */
    public static PropertyMedia video(String mediaUrl) {
        return new PropertyMedia(null, mediaUrl, MediaType.VIDEO, LocalDateTime.now());
    }

    /**
     * Checks if this media is an image.
     *
     * @return {@code true} if the media type is IMAGE, {@code false} otherwise
     */
    public boolean isImage() {
        return MediaType.IMAGE.equals(this.mediaType);
    }

    /**
     * Checks if this media is a video.
     *
     * @return {@code true} if the media type is VIDEO, {@code false} otherwise
     */
    public boolean isVideo() {
        return MediaType.VIDEO.equals(this.mediaType);
    }
}
