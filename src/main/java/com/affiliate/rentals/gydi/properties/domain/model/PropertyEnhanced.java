package com.affiliate.rentals.gydi.properties.domain.model;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * PropertyEnhanced domain entity representing a rental property with full features.
 *
 * <p>This is an immutable domain entity following Domain-Driven Design (DDD) and SOLID principles.
 * It represents a complete rental property with all associated data including location, pricing,
 * amenities, media content, ownership information, and engagement metrics. The class enforces
 * business rules through validation and provides methods for property management operations.</p>
 *
 * <p>Key features:</p>
 * <ul>
 *   <li>Immutability through defensive copying and unmodifiable collections</li>
 *   <li>Builder pattern for flexible construction</li>
 *   <li>Business logic encapsulation (activation, featuring, owner management)</li>
 *   <li>Support for amenities, media, and multiple owners</li>
 *   <li>Engagement tracking (views, ratings, reviews)</li>
 * </ul>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * PropertyEnhanced property = PropertyEnhanced.builder()
 *     .title("Cozy Beach House")
 *     .location("123 Ocean Drive, Miami Beach")
 *     .pricePerNight(Money.of(150, "USD"))
 *     .details(PropertyDetails.of(3, 2, 4, 6))
 *     .addOwnerId(1L)
 *     .addAmenity(Amenity.withName("WiFi"))
 *     .build();
 *
 * PropertyEnhanced featured = property.feature();
 * PropertyEnhanced withViews = property.incrementViews();
 * }</pre>
 *
 * @author GYDI Development Team
 * @see Location
 * @see Money
 * @see PropertyDetails
 * @see Amenity
 * @see PropertyMedia
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
    private final boolean isActive;
    private final boolean isFeatured;
    private final long viewsCount;
    private final Double ratingAvg;
    private final int reviewsCount;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    /**
     * Private constructor used by the Builder.
     * Validates all required fields and creates an immutable PropertyEnhanced instance.
     *
     * @param builder the builder instance containing all property data
     * @throws NullPointerException if any required field is null
     * @throws IllegalArgumentException if title is blank or ownerIds is empty
     */
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
        this.isActive = builder.isActive;
        this.isFeatured = builder.isFeatured;
        this.viewsCount = builder.viewsCount;
        this.ratingAvg = builder.ratingAvg;
        this.reviewsCount = builder.reviewsCount;
        this.createdAt = Objects.requireNonNullElseGet(builder.createdAt, LocalDateTime::now);
        this.updatedAt = Objects.requireNonNullElseGet(builder.updatedAt, LocalDateTime::now);

        validate();
    }

    /**
     * Validates business rules for the property.
     *
     * @throws IllegalArgumentException if validation fails
     */
    private void validate() {
        if (title.isBlank()) {
            throw new IllegalArgumentException("Property title cannot be blank");
        }
        if (ownerIds.isEmpty()) {
            throw new IllegalArgumentException("Property must have at least one owner");
        }
    }

    /**
     * Returns the property's unique identifier.
     *
     * @return the property ID, may be {@code null} for new properties not yet persisted
     */
    public Long id() {
        return id;
    }

    /**
     * Returns the property's title or name.
     *
     * @return the property title, never {@code null} or blank
     */
    public String title() {
        return title;
    }

    /**
     * Returns the property's detailed description.
     *
     * @return the property description, may be {@code null}
     */
    public String description() {
        return description;
    }

    /**
     * Returns the property's location.
     *
     * @return the location, never {@code null}
     */
    public Location location() {
        return location;
    }

    /**
     * Returns the nightly rental price.
     *
     * @return the price per night, never {@code null}
     */
    public Money pricePerNight() {
        return pricePerNight;
    }

    /**
     * Returns the property's physical details.
     *
     * @return the property details, never {@code null}
     */
    public PropertyDetails details() {
        return details;
    }

    /**
     * Returns the URL of the principal/main image.
     *
     * @return the principal image URL, may be {@code null}
     */
    public String principalImage() {
        return principalImage;
    }

    /**
     * Returns an unmodifiable set of the property's amenities.
     *
     * @return the amenities set, never {@code null}
     */
    public Set<Amenity> amenities() {
        return amenities;
    }

    /**
     * Returns an unmodifiable set of the property's media items.
     *
     * @return the media list, never {@code null}
     */
    public Set<PropertyMedia> mediaList() {
        return mediaList;
    }

    /**
     * Returns an unmodifiable set of owner IDs for this property.
     *
     * @return the owner IDs set, never {@code null} or empty
     */
    public Set<Long> ownerIds() {
        return ownerIds;
    }

    /**
     * Checks if the property is active and available for booking.
     *
     * @return {@code true} if active, {@code false} otherwise
     */
    public boolean isActive() {
        return isActive;
    }

    /**
     * Checks if the property is featured for promotional purposes.
     *
     * @return {@code true} if featured, {@code false} otherwise
     */
    public boolean isFeatured() {
        return isFeatured;
    }

    /**
     * Returns the total number of views this property has received.
     *
     * @return the view count
     */
    public long viewsCount() {
        return viewsCount;
    }

    /**
     * Returns the average rating from reviews.
     *
     * @return the average rating, may be {@code null} if no reviews exist
     */
    public Double ratingAvg() {
        return ratingAvg;
    }

    /**
     * Returns the total number of reviews.
     *
     * @return the review count
     */
    public int reviewsCount() {
        return reviewsCount;
    }

    /**
     * Returns the timestamp when this property was created.
     *
     * @return the creation timestamp, never {@code null}
     */
    public LocalDateTime createdAt() {
        return createdAt;
    }

    /**
     * Returns the timestamp when this property was last updated.
     *
     * @return the update timestamp, never {@code null}
     */
    public LocalDateTime updatedAt() {
        return updatedAt;
    }

    /**
     * Checks if the property has a specific amenity.
     *
     * @param amenity the amenity to check
     * @return {@code true} if the property has the amenity, {@code false} otherwise
     */
    public boolean hasAmenity(Amenity amenity) {
        return amenities.contains(amenity);
    }

    /**
     * Creates a new PropertyEnhanced instance with incremented view count.
     * This method maintains immutability by returning a new instance.
     *
     * @return a new PropertyEnhanced instance with views incremented by 1
     */
    public PropertyEnhanced incrementViews() {
        return this.toBuilder().viewsCount(this.viewsCount + 1).build();
    }

    /**
     * Creates a new PropertyEnhanced instance with updated rating information.
     * This method maintains immutability by returning a new instance.
     *
     * @param newRating the new average rating
     * @param totalReviews the new total number of reviews
     * @return a new PropertyEnhanced instance with updated rating data
     */
    public PropertyEnhanced updateRating(double newRating, int totalReviews) {
        return this.toBuilder()
                .ratingAvg(newRating)
                .reviewsCount(totalReviews)
                .build();
    }

    /**
     * Creates a new PropertyEnhanced instance marked as active.
     * This method maintains immutability by returning a new instance.
     *
     * @return a new PropertyEnhanced instance that is active
     */
    public PropertyEnhanced activate() {
        return this.toBuilder().isActive(true).build();
    }

    /**
     * Creates a new PropertyEnhanced instance marked as inactive.
     * This method maintains immutability by returning a new instance.
     *
     * @return a new PropertyEnhanced instance that is inactive
     */
    public PropertyEnhanced deactivate() {
        return this.toBuilder().isActive(false).build();
    }

    /**
     * Creates a new PropertyEnhanced instance marked as featured.
     * This method maintains immutability by returning a new instance.
     *
     * @return a new PropertyEnhanced instance that is featured
     */
    public PropertyEnhanced feature() {
        return this.toBuilder().isFeatured(true).build();
    }

    /**
     * Creates a new PropertyEnhanced instance marked as not featured.
     * This method maintains immutability by returning a new instance.
     *
     * @return a new PropertyEnhanced instance that is not featured
     */
    public PropertyEnhanced unfeature() {
        return this.toBuilder().isFeatured(false).build();
    }

    /**
     * Checks if a specific user is an owner of this property.
     *
     * @param ownerId the owner ID to check
     * @return {@code true} if the user is an owner, {@code false} otherwise
     */
    public boolean hasOwner(Long ownerId) {
        return ownerIds.contains(ownerId);
    }

    /**
     * Creates a new PropertyEnhanced instance with an additional amenity.
     * This method maintains immutability by returning a new instance.
     *
     * @param amenity the amenity to add
     * @return a new PropertyEnhanced instance with the added amenity
     */
    public PropertyEnhanced addAmenity(Amenity amenity) {
        var updatedAmenities = new HashSet<>(this.amenities);
        updatedAmenities.add(amenity);
        return this.toBuilder().amenities(updatedAmenities).build();
    }

    /**
     * Creates a new PropertyEnhanced instance with an amenity removed.
     * This method maintains immutability by returning a new instance.
     *
     * @param amenity the amenity to remove
     * @return a new PropertyEnhanced instance without the specified amenity
     */
    public PropertyEnhanced removeAmenity(Amenity amenity) {
        var updatedAmenities = new HashSet<>(this.amenities);
        updatedAmenities.remove(amenity);
        return this.toBuilder().amenities(updatedAmenities).build();
    }

    /**
     * Creates a new PropertyEnhanced instance with additional media.
     * This method maintains immutability by returning a new instance.
     *
     * @param media the media to add
     * @return a new PropertyEnhanced instance with the added media
     */
    public PropertyEnhanced addMedia(PropertyMedia media) {
        var updatedMedia = new HashSet<>(this.mediaList);
        updatedMedia.add(media);
        return this.toBuilder().mediaList(updatedMedia).build();
    }

    /**
     * Creates a new PropertyEnhanced instance with an additional owner.
     * This method maintains immutability by returning a new instance.
     *
     * @param ownerId the owner ID to add
     * @return a new PropertyEnhanced instance with the added owner
     */
    public PropertyEnhanced addOwner(Long ownerId) {
        var updatedOwners = new HashSet<>(this.ownerIds);
        updatedOwners.add(ownerId);
        return this.toBuilder().ownerIds(updatedOwners).build();
    }

    /**
     * Creates a new PropertyEnhanced instance with an owner removed.
     * This method maintains immutability by returning a new instance.
     *
     * @param ownerId the owner ID to remove
     * @return a new PropertyEnhanced instance without the specified owner
     * @throws IllegalStateException if attempting to remove the last owner
     */
    public PropertyEnhanced removeOwner(Long ownerId) {
        if (ownerIds.size() == 1) {
            throw new IllegalStateException("Cannot remove the last owner from a property");
        }
        var updatedOwners = new HashSet<>(this.ownerIds);
        updatedOwners.remove(ownerId);
        return this.toBuilder().ownerIds(updatedOwners).build();
    }

    /**
     * Creates a new PropertyEnhanced instance with updated pricing.
     * This method maintains immutability by returning a new instance.
     *
     * @param newPrice the new nightly price
     * @return a new PropertyEnhanced instance with the updated price
     */
    public PropertyEnhanced updatePrice(Money newPrice) {
        return this.toBuilder().pricePerNight(newPrice).build();
    }

    /**
     * Creates a new Builder instance for constructing PropertyEnhanced objects.
     *
     * @return a new Builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates a Builder instance initialized with this property's current values.
     * Useful for creating modified copies of the property.
     *
     * @return a Builder instance with all current values set
     */
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
                .isActive(this.isActive)
                .isFeatured(this.isFeatured)
                .viewsCount(this.viewsCount)
                .ratingAvg(this.ratingAvg)
                .reviewsCount(this.reviewsCount)
                .createdAt(this.createdAt)
                .updatedAt(this.updatedAt);
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

    /**
     * Builder class for constructing {@link PropertyEnhanced} instances.
     *
     * <p>This builder follows the fluent interface pattern and provides
     * methods for setting all property attributes. It validates required fields
     * when {@link #build()} is called and sets sensible defaults for optional fields.</p>
     */
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
        private boolean isActive = true;
        private boolean isFeatured = false;
        private long viewsCount = 0;
        private Double ratingAvg;
        private int reviewsCount = 0;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        private Builder() {
        }

        /**
         * Sets the property ID.
         *
         * @param id the property ID
         * @return this builder instance
         */
        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        /**
         * Sets the property title.
         *
         * @param title the property title
         * @return this builder instance
         */
        public Builder title(String title) {
            this.title = title;
            return this;
        }

        /**
         * Sets the property description.
         *
         * @param description the property description
         * @return this builder instance
         */
        public Builder description(String description) {
            this.description = description;
            return this;
        }

        /**
         * Sets the property location using a {@link Location} value object.
         *
         * @param location the location value object
         * @return this builder instance
         */
        public Builder location(Location location) {
            this.location = location;
            return this;
        }

        /**
         * Sets the property location using a string address.
         * The address will be wrapped in a {@link Location} value object.
         *
         * @param locationText the location address string
         * @return this builder instance
         */
        public Builder location(String locationText) {
            this.location = Location.of(locationText);
            return this;
        }

        /**
         * Sets the nightly rental price.
         *
         * @param pricePerNight the price per night
         * @return this builder instance
         */
        public Builder pricePerNight(Money pricePerNight) {
            this.pricePerNight = pricePerNight;
            return this;
        }

        /**
         * Sets the property's physical details.
         *
         * @param details the property details
         * @return this builder instance
         */
        public Builder details(PropertyDetails details) {
            this.details = details;
            return this;
        }

        /**
         * Sets the principal image URL.
         *
         * @param principalImage the principal image URL
         * @return this builder instance
         */
        public Builder principalImage(String principalImage) {
            this.principalImage = principalImage;
            return this;
        }

        /**
         * Sets the amenities as a complete set.
         *
         * @param amenities the set of amenities
         * @return this builder instance
         */
        public Builder amenities(Set<Amenity> amenities) {
            this.amenities = amenities;
            return this;
        }

        /**
         * Adds a single amenity to the amenities set.
         *
         * @param amenity the amenity to add
         * @return this builder instance
         */
        public Builder addAmenity(Amenity amenity) {
            this.amenities.add(amenity);
            return this;
        }

        /**
         * Sets the media list as a complete set.
         *
         * @param mediaList the set of property media
         * @return this builder instance
         */
        public Builder mediaList(Set<PropertyMedia> mediaList) {
            this.mediaList = mediaList;
            return this;
        }

        /**
         * Adds a single media item to the media list.
         *
         * @param media the media to add
         * @return this builder instance
         */
        public Builder addMedia(PropertyMedia media) {
            this.mediaList.add(media);
            return this;
        }

        /**
         * Sets the owner IDs as a complete set.
         *
         * @param ownerIds the set of owner IDs
         * @return this builder instance
         */
        public Builder ownerIds(Set<Long> ownerIds) {
            this.ownerIds = ownerIds;
            return this;
        }

        /**
         * Adds a single owner ID to the owners set.
         *
         * @param ownerId the owner ID to add
         * @return this builder instance
         */
        public Builder addOwnerId(Long ownerId) {
            this.ownerIds.add(ownerId);
            return this;
        }

        /**
         * Sets whether the property is active.
         *
         * @param isActive {@code true} to activate, {@code false} to deactivate
         * @return this builder instance
         */
        public Builder isActive(boolean isActive) {
            this.isActive = isActive;
            return this;
        }

        /**
         * Sets whether the property is featured.
         *
         * @param isFeatured {@code true} to feature, {@code false} otherwise
         * @return this builder instance
         */
        public Builder isFeatured(boolean isFeatured) {
            this.isFeatured = isFeatured;
            return this;
        }

        /**
         * Sets the view count.
         *
         * @param viewsCount the number of views
         * @return this builder instance
         */
        public Builder viewsCount(long viewsCount) {
            this.viewsCount = viewsCount;
            return this;
        }

        /**
         * Sets the average rating.
         *
         * @param ratingAvg the average rating
         * @return this builder instance
         */
        public Builder ratingAvg(Double ratingAvg) {
            this.ratingAvg = ratingAvg;
            return this;
        }

        /**
         * Sets the review count.
         *
         * @param reviewsCount the number of reviews
         * @return this builder instance
         */
        public Builder reviewsCount(int reviewsCount) {
            this.reviewsCount = reviewsCount;
            return this;
        }

        /**
         * Sets the creation timestamp.
         *
         * @param createdAt the creation timestamp
         * @return this builder instance
         */
        public Builder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        /**
         * Sets the update timestamp.
         *
         * @param updatedAt the update timestamp
         * @return this builder instance
         */
        public Builder updatedAt(LocalDateTime updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        /**
         * Builds and returns a new {@link PropertyEnhanced} instance.
         *
         * @return the constructed PropertyEnhanced
         * @throws NullPointerException if any required field is null
         * @throws IllegalArgumentException if title is blank or ownerIds is empty
         */
        public PropertyEnhanced build() {
            return new PropertyEnhanced(this);
        }
    }
}
