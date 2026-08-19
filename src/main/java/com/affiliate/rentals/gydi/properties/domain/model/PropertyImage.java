package com.affiliate.rentals.gydi.properties.domain.model;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * PropertyImage entity representing an image associated with a property.
 * Part of the Property aggregate.
 */
public class PropertyImage {
    private Long id;
    private final PropertyId propertyId;
    private final String url;
    private int displayOrder; // Mutable for reordering
    private final LocalDateTime uploadedAt;

    private PropertyImage(Long id, PropertyId propertyId, String url, int displayOrder, LocalDateTime uploadedAt) {
        this.id = id;
        this.propertyId = Objects.requireNonNull(propertyId, "PropertyId cannot be null");
        this.url = Objects.requireNonNull(url, "URL cannot be null");
        this.displayOrder = displayOrder;
        this.uploadedAt = Objects.requireNonNullElseGet(uploadedAt, LocalDateTime::now);

        validate();
    }

    public static PropertyImage create(PropertyId propertyId, String url, int displayOrder) {
        return new PropertyImage(null, propertyId, url, displayOrder, LocalDateTime.now());
    }

    public static PropertyImage of(Long id, PropertyId propertyId, String url, int displayOrder, LocalDateTime uploadedAt) {
        return new PropertyImage(id, propertyId, url, displayOrder, uploadedAt);
    }

    private void validate() {
        if (url.isBlank()) {
            throw new IllegalArgumentException("Image URL cannot be blank");
        }
        if (displayOrder < 0) {
            throw new IllegalArgumentException("Display order cannot be negative");
        }
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

    public int getDisplayOrder() {
        return displayOrder;
    }

    public LocalDateTime getUploadedAt() {
        return uploadedAt;
    }

    /**
     * Updates the display order of this image.
     * Package-private to ensure only Property aggregate can modify.
     *
     * @param newOrder the new display order
     * @throws IllegalArgumentException if order is negative
     */
    void setDisplayOrder(int newOrder) {
        if (newOrder < 0) {
            throw new IllegalArgumentException("Display order cannot be negative");
        }
        this.displayOrder = newOrder;
    }

    /**
     * Checks if this image is the main/cover image.
     *
     * @return true if display order is 0 (cover image)
     */
    public boolean isMainImage() {
        return displayOrder == 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PropertyImage that = (PropertyImage) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "PropertyImage[id=%s, propertyId=%s, url=%s, order=%d]"
                .formatted(id, propertyId, url, displayOrder);
    }
}
