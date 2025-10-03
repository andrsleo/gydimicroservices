package com.affiliate.rentals.gydi.properties.domain.model;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * PropertyMedia value object - represents media associated with a property.
 * Immutable by design following DDD principles.
 */
public record PropertyMedia(
        Long id,
        String mediaUrl,
        MediaType mediaType,
        LocalDateTime createdAt
) {

    public PropertyMedia {
        Objects.requireNonNull(mediaUrl, "Media URL cannot be null");
        Objects.requireNonNull(mediaType, "Media type cannot be null");
        if (mediaUrl.isBlank()) {
            throw new IllegalArgumentException("Media URL cannot be blank");
        }
        createdAt = Objects.requireNonNullElseGet(createdAt, LocalDateTime::now);
    }

    public static PropertyMedia of(Long id, String mediaUrl, MediaType mediaType, LocalDateTime createdAt) {
        return new PropertyMedia(id, mediaUrl, mediaType, createdAt);
    }

    public static PropertyMedia image(String mediaUrl) {
        return new PropertyMedia(null, mediaUrl, MediaType.IMAGE, LocalDateTime.now());
    }

    public static PropertyMedia video(String mediaUrl) {
        return new PropertyMedia(null, mediaUrl, MediaType.VIDEO, LocalDateTime.now());
    }

    public boolean isImage() {
        return MediaType.IMAGE.equals(this.mediaType);
    }

    public boolean isVideo() {
        return MediaType.VIDEO.equals(this.mediaType);
    }
}
