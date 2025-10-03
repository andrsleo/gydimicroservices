package com.affiliate.rentals.gydi.properties.domain.model;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Property domain entity - represents a rental property.
 * Enhanced version with amenities, media, and owners.
 * Immutable by design following clean code and SOLID principles.
 */
public final class PropertyEnhanced {
    private final Long id;
    private final String title;
    private final String description;
    private final Location location;
    private final Money pricePerNight;
    private final PropertyDetails details;
    private final String principalImage;
    private final Set<Amenity> amenities;
    private final Set<PropertyMedia> mediaList;
    private final Set<Long> ownerIds;
    private final LocalDateTime createdAt;

    private PropertyEnhanced(Builder builder) {
        this.id = builder.id;
        this.title = Objects.requireNonNull(builder.title, "Title cannot be null");
        this.description = builder.description;
        this.location = Objects.requireNonNull(builder.location, "Location cannot be null");
        this.pricePerNight = Objects.requireNonNull(builder.pricePerNight, "Price per night cannot be null");
        this.details = Objects.requireNonNull(builder.details, "Property details cannot be null");
        this.principalImage = builder.principalImage;
        this.amenities = Collections.unmodifiableSet(new HashSet<>(builder.amenities));
        this.mediaList = Collections.unmodifiableSet(new HashSet<>(builder.mediaList));
        this.ownerIds = Collections.unmodifiableSet(new HashSet<>(builder.ownerIds));
        this.createdAt = Objects.requireNonNullElseGet(builder.createdAt, LocalDateTime::now);

        validate();
    }

    private void validate() {
        if (title.isBlank()) {
            throw new IllegalArgumentException("Property title cannot be blank");
        }
        if (ownerIds.isEmpty()) {
            throw new IllegalArgumentException("Property must have at least one owner");
        }
    }

    public Long id() {
        return id;
    }

    public String title() {
        return title;
    }

    public String description() {
        return description;
    }

    public Location location() {
        return location;
    }

    public Money pricePerNight() {
        return pricePerNight;
    }

    public PropertyDetails details() {
        return details;
    }

    public String principalImage() {
        return principalImage;
    }

    public Set<Amenity> amenities() {
        return amenities;
    }

    public Set<PropertyMedia> mediaList() {
        return mediaList;
    }

    public Set<Long> ownerIds() {
        return ownerIds;
    }

    public LocalDateTime createdAt() {
        return createdAt;
    }

    public boolean hasAmenity(Amenity amenity) {
        return amenities.contains(amenity);
    }

    public boolean hasOwner(Long ownerId) {
        return ownerIds.contains(ownerId);
    }

    public PropertyEnhanced addAmenity(Amenity amenity) {
        var updatedAmenities = new HashSet<>(this.amenities);
        updatedAmenities.add(amenity);
        return this.toBuilder().amenities(updatedAmenities).build();
    }

    public PropertyEnhanced removeAmenity(Amenity amenity) {
        var updatedAmenities = new HashSet<>(this.amenities);
        updatedAmenities.remove(amenity);
        return this.toBuilder().amenities(updatedAmenities).build();
    }

    public PropertyEnhanced addMedia(PropertyMedia media) {
        var updatedMedia = new HashSet<>(this.mediaList);
        updatedMedia.add(media);
        return this.toBuilder().mediaList(updatedMedia).build();
    }

    public PropertyEnhanced addOwner(Long ownerId) {
        var updatedOwners = new HashSet<>(this.ownerIds);
        updatedOwners.add(ownerId);
        return this.toBuilder().ownerIds(updatedOwners).build();
    }

    public PropertyEnhanced removeOwner(Long ownerId) {
        if (ownerIds.size() == 1) {
            throw new IllegalStateException("Cannot remove the last owner from a property");
        }
        var updatedOwners = new HashSet<>(this.ownerIds);
        updatedOwners.remove(ownerId);
        return this.toBuilder().ownerIds(updatedOwners).build();
    }

    public PropertyEnhanced updatePrice(Money newPrice) {
        return this.toBuilder().pricePerNight(newPrice).build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public Builder toBuilder() {
        return new Builder()
                .id(this.id)
                .title(this.title)
                .description(this.description)
                .location(this.location)
                .pricePerNight(this.pricePerNight)
                .details(this.details)
                .principalImage(this.principalImage)
                .amenities(new HashSet<>(this.amenities))
                .mediaList(new HashSet<>(this.mediaList))
                .ownerIds(new HashSet<>(this.ownerIds))
                .createdAt(this.createdAt);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PropertyEnhanced that = (PropertyEnhanced) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Property[id=%d, title=%s, location=%s, price=%s]"
                .formatted(id, title, location, pricePerNight);
    }

    public static final class Builder {
        private Long id;
        private String title;
        private String description;
        private Location location;
        private Money pricePerNight;
        private PropertyDetails details;
        private String principalImage;
        private Set<Amenity> amenities = new HashSet<>();
        private Set<PropertyMedia> mediaList = new HashSet<>();
        private Set<Long> ownerIds = new HashSet<>();
        private LocalDateTime createdAt;

        private Builder() {
        }

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder location(Location location) {
            this.location = location;
            return this;
        }

        public Builder location(String locationText) {
            this.location = Location.of(locationText);
            return this;
        }

        public Builder pricePerNight(Money pricePerNight) {
            this.pricePerNight = pricePerNight;
            return this;
        }

        public Builder details(PropertyDetails details) {
            this.details = details;
            return this;
        }

        public Builder principalImage(String principalImage) {
            this.principalImage = principalImage;
            return this;
        }

        public Builder amenities(Set<Amenity> amenities) {
            this.amenities = amenities;
            return this;
        }

        public Builder addAmenity(Amenity amenity) {
            this.amenities.add(amenity);
            return this;
        }

        public Builder mediaList(Set<PropertyMedia> mediaList) {
            this.mediaList = mediaList;
            return this;
        }

        public Builder addMedia(PropertyMedia media) {
            this.mediaList.add(media);
            return this;
        }

        public Builder ownerIds(Set<Long> ownerIds) {
            this.ownerIds = ownerIds;
            return this;
        }

        public Builder addOwnerId(Long ownerId) {
            this.ownerIds.add(ownerId);
            return this;
        }

        public Builder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public PropertyEnhanced build() {
            return new PropertyEnhanced(this);
        }
    }
}
