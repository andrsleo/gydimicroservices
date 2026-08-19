package com.affiliate.rentals.gydi.properties.domain.model;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * PropertyVideo entity representing a video associated with a property.
 * Part of the Property aggregate.
 */
public class PropertyVideo {
    private Long id;
    private final PropertyId propertyId;
    private final String url;
    private final String thumbnailUrl;
    private int displayOrder;
    private final Integer durationSeconds;
    private final LocalDateTime uploadedAt;

    private PropertyVideo(Long id, PropertyId propertyId, String url, String thumbnailUrl,
                          int displayOrder, Integer durationSeconds, LocalDateTime uploadedAt) {
        this.id = id;
        this.propertyId = Objects.requireNonNull(propertyId, "PropertyId cannot be null");
        this.url = Objects.requireNonNull(url, "URL cannot be null");
        this.thumbnailUrl = thumbnailUrl;
        this.displayOrder = displayOrder;
        this.durationSeconds = durationSeconds;
        this.uploadedAt = Objects.requireNonNullElseGet(uploadedAt, LocalDateTime::now);

        validate();
    }

    public static PropertyVideo create(PropertyId propertyId, String url, int displayOrder) {
        return new PropertyVideo(null, propertyId, url, null, displayOrder, null, LocalDateTime.now());
    }

    public static PropertyVideo create(PropertyId propertyId, String url, String thumbnailUrl, int displayOrder, Integer durationSeconds) {
        return new PropertyVideo(null, propertyId, url, thumbnailUrl, displayOrder, durationSeconds, LocalDateTime.now());
    }

    public static PropertyVideo of(Long id, PropertyId propertyId, String url, String thumbnailUrl,
                                   int displayOrder, Integer durationSeconds, LocalDateTime uploadedAt) {
        return new PropertyVideo(id, propertyId, url, thumbnailUrl, displayOrder, durationSeconds, uploadedAt);
    }

    private void validate() {
        if (url.isBlank()) {
            throw new IllegalArgumentException("Video URL cannot be blank");
        }
        if (displayOrder < 0) {
            throw new IllegalArgumentException("Display order cannot be negative");
        }
        if (durationSeconds != null && durationSeconds < 0) {
            throw new IllegalArgumentException("Duration cannot be negative");
        }
    }

    public PropertyVideo withThumbnail(String thumbnailUrl) {
        return new PropertyVideo(this.id, this.propertyId, this.url, thumbnailUrl, 
                                this.displayOrder, this.durationSeconds, this.uploadedAt);
    }

    public PropertyVideo withDuration(int durationSeconds) {
        return new PropertyVideo(this.id, this.propertyId, this.url, this.thumbnailUrl, 
                                this.displayOrder, durationSeconds, this.uploadedAt);
    }

    public Long getId() {
        return id;
    }

    public PropertyId getPropertyId() {
        return propertyId;
    }

    public String getUrl() {
        return url;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public int getDisplayOrder() {
        return displayOrder;
    }

    void setDisplayOrder(int newOrder) {
        if (newOrder < 0) {
            throw new IllegalArgumentException("Display order cannot be negative");
        }
        this.displayOrder = newOrder;
    }

    public Integer getDurationSeconds() {
        return durationSeconds;
    }

    public LocalDateTime getUploadedAt() {
        return uploadedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PropertyVideo that = (PropertyVideo) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "PropertyVideo[id=%s, propertyId=%s, url=%s, order=%d, duration=%ds]"
                .formatted(id, propertyId, url, displayOrder, durationSeconds);
    }
}
