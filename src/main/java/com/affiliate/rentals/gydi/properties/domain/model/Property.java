package com.affiliate.rentals.gydi.properties.domain.model;

import com.affiliate.rentals.gydi.properties.domain.exception.PropertyCannotBePublishedException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Property aggregate root representing a vacation rental property.
 * Enforces business rules and manages PropertyImage and PropertyVideo entities.
 */
public class Property {
    private static final int MAX_IMAGES = 20;
    private static final int MAX_VIDEOS = 2;
    private static final int MIN_IMAGES_FOR_PUBLISH = 4;

    private final PropertyId id;
    private Long hostId;
    private String title;
    private String slug; // SEO-friendly URL slug (e.g., "beach-house-malibu-x7k2m")
    private String description;
    private Money pricePerNight;
    private Money salePrice; // Price for sale (used when listingType allows sale)
    private PropertyLocation location;
    private List<String> amenities;
    private PropertySpecs specs;
    private PropertyType propertyType;
    private PropertyListingType listingType;
    private PropertyStatus status;
    private final List<PropertyImage> images;
    private final List<PropertyVideo> videos;
    private Long coverImageId; // Main/cover image for listings
    // Airbnb import fields
    private String airbnbUrl;
    private ImportMode importMode;
    private LocalDateTime importedAt;
    private String airbnbListingId;
    private String icalUrlAirbnb;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime publishedAt;

    private Property(Builder builder) {
        // Allow null ID for new entities (will be assigned by repository/database)
        this.id = builder.id;
        this.hostId = Objects.requireNonNull(builder.hostId, "Host ID cannot be null");
        this.title = Objects.requireNonNull(builder.title, "Title cannot be null");
        this.slug = builder.slug; // Optional, can be generated later
        this.description = builder.description;
        this.pricePerNight = Objects.requireNonNull(builder.pricePerNight, "Price per night cannot be null");
        this.salePrice = builder.salePrice; // Optional, used when listingType allows sale
        this.location = Objects.requireNonNull(builder.location, "Location cannot be null");
        this.amenities = builder.amenities != null ? new ArrayList<>(builder.amenities) : new ArrayList<>();
        this.specs = Objects.requireNonNull(builder.specs, "Property specs cannot be null");
        this.propertyType = Objects.requireNonNull(builder.propertyType, "Property type cannot be null");
        this.listingType = builder.listingType != null ? builder.listingType : PropertyListingType.SHORT_TERM_RENTAL;
        this.status = builder.status != null ? builder.status : PropertyStatus.DRAFT;
        this.images = builder.images != null ? new ArrayList<>(builder.images) : new ArrayList<>();
        this.videos = builder.videos != null ? new ArrayList<>(builder.videos) : new ArrayList<>();
        this.coverImageId = builder.coverImageId;
        this.airbnbUrl = builder.airbnbUrl;
        this.importMode = builder.importMode != null ? builder.importMode : ImportMode.MANUAL;
        this.importedAt = builder.importedAt;
        this.airbnbListingId = builder.airbnbListingId;
        this.icalUrlAirbnb = builder.icalUrlAirbnb;
        this.createdAt = builder.createdAt != null ? builder.createdAt : LocalDateTime.now();
        this.updatedAt = builder.updatedAt != null ? builder.updatedAt : LocalDateTime.now();
        this.publishedAt = builder.publishedAt;

        validate();
    }

    private void validate() {
        if (title.isBlank()) {
            throw new IllegalArgumentException("Title cannot be blank");
        }
        if (title.length() < 10 || title.length() > 100) {
            throw new IllegalArgumentException("Title must be between 10 and 100 characters");
        }
        if (description != null && description.length() > 2000) {
            throw new IllegalArgumentException("Description cannot exceed 2000 characters");
        }
        if (images.size() > MAX_IMAGES) {
            throw new IllegalArgumentException("Cannot have more than " + MAX_IMAGES + " images");
        }
        if (videos.size() > MAX_VIDEOS) {
            throw new IllegalArgumentException("Cannot have more than " + MAX_VIDEOS + " videos");
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Publishes this property, making it available to the public.
     * Validates all required fields before publishing.
     *
     * @throws PropertyCannotBePublishedException if property doesn't meet
     *                                            publication requirements
     */
    public void publish() {
        List<String> errors = collectPublishValidationErrors();

        if (!errors.isEmpty()) {
            throw new PropertyCannotBePublishedException(errors);
        }

        this.status = PropertyStatus.PUBLISHED;
        this.publishedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Checks if this property can be published.
     *
     * @return true if all publication requirements are met
     */
    public boolean canBePublished() {
        return collectPublishValidationErrors().isEmpty();
    }

    /**
     * Collects all validation errors that prevent publishing.
     *
     * @return list of validation error messages
     */
    private List<String> collectPublishValidationErrors() {
        List<String> errors = new ArrayList<>();

        validateImages(errors);
        validateBasicInfo(errors);
        validatePricing(errors);
        validateLocation(errors);
        validateSpecs(errors);
        validateAmenities(errors);
        validateListingType(errors);
        validateStatus(errors);

        return errors;
    }

    private void validateImages(List<String> errors) {
        if (images.size() < MIN_IMAGES_FOR_PUBLISH) {
            errors.add(String.format("Property must have at least %d images (currently has %d)",
                    MIN_IMAGES_FOR_PUBLISH, images.size()));
        }
    }

    private void validateBasicInfo(List<String> errors) {
        if (title == null || title.isBlank()) {
            errors.add("Title is required");
        }
        if (description == null || description.isBlank()) {
            errors.add("Description is required");
        }
    }

    private void validatePricing(List<String> errors) {
        if (pricePerNight == null) {
            errors.add("Price per night is required");
        }
        if (propertyType == null) {
            errors.add("Property type is required");
        }
    }

    private void validateLocation(List<String> errors) {
        if (location == null) {
            errors.add("Location is required");
            return;
        }

        if (location.country() == null || location.country().isBlank()) {
            errors.add("Country is required");
        }
        if (location.city() == null || location.city().isBlank()) {
            errors.add("City is required");
        }
        if (location.address() == null || location.address().isBlank()) {
            errors.add("Address is required");
        }
        if (location.postalCode() == null || location.postalCode().isBlank()) {
            errors.add("Postal code is required");
        }
    }

    private void validateSpecs(List<String> errors) {
        if (specs == null) {
            errors.add("Property specifications are required");
            return;
        }

        if (specs.bedrooms() <= 0) {
            errors.add("Property must have at least 1 bedroom");
        }
        if (specs.bathrooms() <= 0) {
            errors.add("Property must have at least 1 bathroom");
        }
        if (specs.maxGuests() <= 0) {
            errors.add("Property must accommodate at least 1 guest");
        }
    }

    private void validateAmenities(List<String> errors) {
        if (amenities == null || amenities.isEmpty()) {
            errors.add("Property must have at least 1 amenity");
        }
    }

    private void validateListingType(List<String> errors) {
        if (listingType == null) {
            errors.add("Listing type is required");
            return;
        }

        // Validate sale price for sale listings
        if (listingType.allowsSale()
                && (salePrice == null || salePrice.amount().compareTo(java.math.BigDecimal.ZERO) <= 0)) {
            errors.add("Sale price is required for properties available for sale");
        }

        // Validate rental price for rental listings
        if (listingType.allowsRental()
                && (pricePerNight == null || pricePerNight.amount().compareTo(java.math.BigDecimal.ZERO) <= 0)) {
            errors.add("Price per night is required for rental properties");
        }

        // Validate specs for rental listings
        if (listingType.allowsRental() && (specs == null || specs.maxGuests() <= 0)) {
            errors.add("Max guests is required for rental properties");
        }
    }

    private void validateStatus(List<String> errors) {
        if (status != PropertyStatus.DRAFT) {
            errors.add("Only properties in DRAFT status can be published");
        }
    }

    public void addImage(String url, int displayOrder) {
        if (images.size() >= MAX_IMAGES) {
            throw new IllegalStateException("Maximum number of images (" + MAX_IMAGES + ") reached");
        }
        // Auto-calculate displayOrder if -1 or if conflicts with existing order
        int finalDisplayOrder;
        if (displayOrder == -1) {
            // Calculate next available displayOrder (max + 1)
            finalDisplayOrder = images.stream()
                    .mapToInt(PropertyImage::getDisplayOrder)
                    .max()
                    .orElse(-1) + 1;
        } else {
            // Check if displayOrder already exists
            boolean orderExists = images.stream()
                    .anyMatch(img -> img.getDisplayOrder() == displayOrder);
            if (orderExists) {
                // Calculate next available displayOrder to avoid conflict
                finalDisplayOrder = images.stream()
                        .mapToInt(PropertyImage::getDisplayOrder)
                        .max()
                        .orElse(-1) + 1;
            } else {
                finalDisplayOrder = displayOrder;
            }
        }
        PropertyImage image = PropertyImage.create(this.id, url, finalDisplayOrder);
        this.images.add(image);
        this.updatedAt = LocalDateTime.now();
    }

    public void addVideo(String url, int displayOrder) {
        if (videos.size() >= MAX_VIDEOS) {
            throw new IllegalStateException("Maximum number of videos (" + MAX_VIDEOS + ") reached");
        }
        // Auto-calculate displayOrder if -1 or if conflicts with existing order
        int finalDisplayOrder;
        if (displayOrder == -1) {
            // Calculate next available displayOrder (max + 1)
            finalDisplayOrder = videos.stream()
                    .mapToInt(PropertyVideo::getDisplayOrder)
                    .max()
                    .orElse(-1) + 1;
        } else {
            // Check if displayOrder already exists
            boolean orderExists = videos.stream()
                    .anyMatch(vid -> vid.getDisplayOrder() == displayOrder);
            if (orderExists) {
                // Calculate next available displayOrder to avoid conflict
                finalDisplayOrder = videos.stream()
                        .mapToInt(PropertyVideo::getDisplayOrder)
                        .max()
                        .orElse(-1) + 1;
            } else {
                finalDisplayOrder = displayOrder;
            }
        }
        PropertyVideo video = PropertyVideo.create(this.id, url, finalDisplayOrder);
        this.videos.add(video);
        this.updatedAt = LocalDateTime.now();
    }

    public void addVideo(String url, String thumbnailUrl, int displayOrder, Integer durationSeconds) {
        if (videos.size() >= MAX_VIDEOS) {
            throw new IllegalStateException("Maximum number of videos (" + MAX_VIDEOS + ") reached");
        }
        // Auto-calculate displayOrder if -1 or if conflicts with existing order
        int finalDisplayOrder;
        if (displayOrder == -1) {
            // Calculate next available displayOrder (max + 1)
            finalDisplayOrder = videos.stream()
                    .mapToInt(PropertyVideo::getDisplayOrder)
                    .max()
                    .orElse(-1) + 1;
        } else {
            // Check if displayOrder already exists
            boolean orderExists = videos.stream()
                    .anyMatch(vid -> vid.getDisplayOrder() == displayOrder);
            if (orderExists) {
                // Calculate next available displayOrder to avoid conflict
                finalDisplayOrder = videos.stream()
                        .mapToInt(PropertyVideo::getDisplayOrder)
                        .max()
                        .orElse(-1) + 1;
            } else {
                finalDisplayOrder = displayOrder;
            }
        }
        PropertyVideo video = PropertyVideo.create(this.id, url, thumbnailUrl, finalDisplayOrder, durationSeconds);
        this.videos.add(video);
        this.updatedAt = LocalDateTime.now();
    }

    public void removeImage(Long imageId) {
        boolean removed = images.removeIf(img -> img.getId().equals(imageId));
        if (removed) {
            this.updatedAt = LocalDateTime.now();
        }
    }

    public void removeVideo(Long videoId) {
        boolean removed = videos.removeIf(vid -> vid.getId().equals(videoId));
        if (removed) {
            this.updatedAt = LocalDateTime.now();
        }
    }

    /**
     * Reorders property images based on provided mapping.
     * Ensures display orders are consecutive starting from 0.
     *
     * @param imageOrders map of image ID to new display order
     * @throws IllegalArgumentException if validation fails
     */
    public void reorderImages(java.util.Map<Long, Integer> imageOrders) {
        if (imageOrders.isEmpty()) {
            throw new IllegalArgumentException("Cannot reorder with empty orders");
        }

        // Validate all current image IDs are in the map
        java.util.Set<Long> currentIds = images.stream()
                .map(PropertyImage::getId)
                .collect(java.util.stream.Collectors.toSet());

        if (!imageOrders.keySet().equals(currentIds)) {
            throw new IllegalArgumentException("Image IDs in reorder request do not match current images");
        }

        // Validate no duplicate display orders
        long distinctOrders = imageOrders.values().stream().distinct().count();
        if (distinctOrders != imageOrders.size()) {
            throw new IllegalArgumentException("Duplicate display orders detected");
        }

        // Apply new orders
        for (PropertyImage image : this.images) {
            Integer newOrder = imageOrders.get(image.getId());
            image.setDisplayOrder(newOrder);
        }

        // Sort list to maintain consistency
        this.images.sort(java.util.Comparator.comparingInt(PropertyImage::getDisplayOrder));
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Reorders property videos based on provided mapping.
     *
     * @param videoOrders map of video ID to new display order
     * @throws IllegalArgumentException if validation fails
     */
    public void reorderVideos(java.util.Map<Long, Integer> videoOrders) {
        if (videoOrders.isEmpty()) {
            throw new IllegalArgumentException("Cannot reorder with empty orders");
        }
        java.util.Set<Long> currentIds = videos.stream()
                .map(PropertyVideo::getId)
                .collect(java.util.stream.Collectors.toSet());
        if (!videoOrders.keySet().equals(currentIds)) {
            throw new IllegalArgumentException("Video IDs in reorder request do not match current videos");
        }
        long distinctOrders = videoOrders.values().stream().distinct().count();
        if (distinctOrders != videoOrders.size()) {
            throw new IllegalArgumentException("Duplicate display orders detected");
        }
        for (PropertyVideo video : this.videos) {
            Integer newOrder = videoOrders.get(video.getId());
            video.setDisplayOrder(newOrder);
        }
        this.videos.sort(java.util.Comparator.comparingInt(PropertyVideo::getDisplayOrder));
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Sets the cover/main image for this property.
     * The cover image is displayed in property listings.
     *
     * @param imageId the ID of the image to set as cover, or null to clear
     * @throws IllegalArgumentException if imageId is not null and image not found
     */
    public void setCoverImage(Long imageId) {
        // Allow null to clear cover image
        if (imageId == null) {
            this.coverImageId = null;
            this.updatedAt = LocalDateTime.now();
            return;
        }

        if (images.isEmpty()) {
            throw new IllegalStateException("Cannot set cover image when property has no images");
        }

        boolean imageExists = images.stream()
                .anyMatch(img -> img.getId().equals(imageId));

        if (!imageExists) {
            throw new IllegalArgumentException("Image not found in property images");
        }

        this.coverImageId = imageId;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Gets the cover/main image.
     * If no cover image is set, returns the first image by display order.
     *
     * @return the cover image, or null if no images exist
     */
    public PropertyImage getCoverImage() {
        if (images.isEmpty()) {
            return null;
        }

        if (coverImageId != null) {
            return images.stream()
                    .filter(img -> img.getId().equals(coverImageId))
                    .findFirst()
                    .orElse(null);
        }

        // Default: first image by displayOrder
        return images.stream()
                .min(java.util.Comparator.comparingInt(PropertyImage::getDisplayOrder))
                .orElse(null);
    }

    /**
     * Gets the cover image ID.
     *
     * @return the cover image ID, or null if not set
     */
    public Long getCoverImageId() {
        return coverImageId;
    }

    public void updateDetails(String title, String description, Money pricePerNight, Money salePrice) {
        if (title != null) {
            if (title.isBlank() || title.length() < 10 || title.length() > 100) {
                throw new IllegalArgumentException("Invalid title");
            }
            this.title = title;
        }
        if (description != null) {
            if (description.length() > 2000) {
                throw new IllegalArgumentException("Description too long");
            }
            this.description = description;
        }
        if (pricePerNight != null) {
            this.pricePerNight = pricePerNight;
        }
        if (salePrice != null) {
            this.salePrice = salePrice;
        }
        this.updatedAt = LocalDateTime.now();
    }

    public void updateLocation(PropertyLocation location) {
        this.location = Objects.requireNonNull(location, "Location cannot be null");
        this.updatedAt = LocalDateTime.now();
    }

    public void updateSpecs(PropertySpecs specs) {
        this.specs = Objects.requireNonNull(specs, "Specs cannot be null");
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Updates the listing type with business validations.
     *
     * @param newListingType the new listing type
     * @throws IllegalArgumentException if newListingType is null
     * @throws IllegalStateException    if transition not allowed
     */
    public void updateListingType(PropertyListingType newListingType) {
        Objects.requireNonNull(newListingType, "Listing type cannot be null");

        if (!this.listingType.canTransitionTo(newListingType)) {
            throw new IllegalStateException(
                    String.format("Cannot change listing type from %s to %s",
                            this.listingType, newListingType));
        }

        this.listingType = newListingType;
        this.updatedAt = LocalDateTime.now();
    }

    public void addAmenity(String amenity) {
        if (!this.amenities.contains(amenity)) {
            this.amenities.add(amenity);
            this.updatedAt = LocalDateTime.now();
        }
    }

    public void removeAmenity(String amenity) {
        if (this.amenities.remove(amenity)) {
            this.updatedAt = LocalDateTime.now();
        }
    }

    /**
     * Clears all amenities from the property.
     * Used when replacing amenities during updates.
     */
    public void clearAmenities() {
        if (!this.amenities.isEmpty()) {
            this.amenities.clear();
            this.updatedAt = LocalDateTime.now();
        }
    }

    /**
     * Updates the property type with validation.
     *
     * @param newPropertyType the new property type
     * @throws IllegalArgumentException if newPropertyType is null
     */
    public void updatePropertyType(PropertyType newPropertyType) {
        Objects.requireNonNull(newPropertyType, "Property type cannot be null");
        this.propertyType = newPropertyType;
        this.updatedAt = LocalDateTime.now();
    }

    public void deactivate() {
        this.status = PropertyStatus.INACTIVE;
        this.updatedAt = LocalDateTime.now();
    }

    public void activate() {
        if (this.status == PropertyStatus.DELETED) {
            throw new IllegalStateException("Cannot activate a deleted property");
        }
        this.status = PropertyStatus.PUBLISHED;
        this.updatedAt = LocalDateTime.now();
    }

    public void delete() {
        this.status = PropertyStatus.DELETED;
        this.updatedAt = LocalDateTime.now();
    }

    public boolean isOwnedBy(Long userId) {
        return this.hostId.equals(userId);
    }

    public int getImageCount() {
        return images.size();
    }

    public int getVideoCount() {
        return videos.size();
    }

    // Getters
    public PropertyId getId() {
        return id;
    }

    public Long getHostId() {
        return hostId;
    }

    public String getTitle() {
        return title;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
        this.updatedAt = LocalDateTime.now();
    }

    public void updateIcalUrlAirbnb(String icalUrlAirbnb) {
        this.icalUrlAirbnb = icalUrlAirbnb;
        this.updatedAt = LocalDateTime.now();
    }

    public String getDescription() {
        return description;
    }

    public Money getPricePerNight() {
        return pricePerNight;
    }

    public Money getSalePrice() {
        return salePrice;
    }

    public PropertyLocation getLocation() {
        return location;
    }

    public List<String> getAmenities() {
        return Collections.unmodifiableList(amenities);
    }

    public PropertySpecs getSpecs() {
        return specs;
    }

    public PropertyType getPropertyType() {
        return propertyType;
    }

    public PropertyListingType getListingType() {
        return listingType;
    }

    public PropertyStatus getStatus() {
        return status;
    }

    public List<PropertyImage> getImages() {
        return Collections.unmodifiableList(images);
    }

    public List<PropertyVideo> getVideos() {
        return Collections.unmodifiableList(videos);
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public LocalDateTime getPublishedAt() {
        return publishedAt;
    }

    public String getAirbnbUrl() {
        return airbnbUrl;
    }

    public ImportMode getImportMode() {
        return importMode;
    }

    public LocalDateTime getImportedAt() {
        return importedAt;
    }

    public String getAirbnbListingId() {
        return airbnbListingId;
    }

    public String getIcalUrlAirbnb() {
        return icalUrlAirbnb;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Property property = (Property) o;
        return Objects.equals(id, property.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Property[id=%s, title=%s, status=%s, images=%d, videos=%d]"
                .formatted(id, title, status, images.size(), videos.size());
    }

    public static class Builder {
        private PropertyId id;
        private Long hostId;
        private String title;
        private String slug;
        private String description;
        private Money pricePerNight;
        private Money salePrice;
        private PropertyLocation location;
        private List<String> amenities;
        private PropertySpecs specs;
        private PropertyType propertyType;
        private PropertyListingType listingType;
        private PropertyStatus status;
        private List<PropertyImage> images;
        private List<PropertyVideo> videos;
        private Long coverImageId;
        private String airbnbUrl;
        private ImportMode importMode;
        private LocalDateTime importedAt;
        private String airbnbListingId;
        private String icalUrlAirbnb;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private LocalDateTime publishedAt;

        public Builder id(PropertyId id) {
            this.id = id;
            return this;
        }

        public Builder hostId(Long hostId) {
            this.hostId = hostId;
            return this;
        }

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder slug(String slug) {
            this.slug = slug;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder pricePerNight(Money pricePerNight) {
            this.pricePerNight = pricePerNight;
            return this;
        }

        public Builder salePrice(Money salePrice) {
            this.salePrice = salePrice;
            return this;
        }

        public Builder location(PropertyLocation location) {
            this.location = location;
            return this;
        }

        public Builder amenities(List<String> amenities) {
            this.amenities = amenities;
            return this;
        }

        public Builder specs(PropertySpecs specs) {
            this.specs = specs;
            return this;
        }

        public Builder propertyType(PropertyType propertyType) {
            this.propertyType = propertyType;
            return this;
        }

        public Builder listingType(PropertyListingType listingType) {
            this.listingType = listingType;
            return this;
        }

        public Builder status(PropertyStatus status) {
            this.status = status;
            return this;
        }

        public Builder images(List<PropertyImage> images) {
            this.images = images;
            return this;
        }

        public Builder videos(List<PropertyVideo> videos) {
            this.videos = videos;
            return this;
        }

        public Builder coverImageId(Long coverImageId) {
            this.coverImageId = coverImageId;
            return this;
        }

        public Builder airbnbUrl(String airbnbUrl) {
            this.airbnbUrl = airbnbUrl;
            return this;
        }

        public Builder importMode(ImportMode importMode) {
            this.importMode = importMode;
            return this;
        }

        public Builder importedAt(LocalDateTime importedAt) {
            this.importedAt = importedAt;
            return this;
        }

        public Builder airbnbListingId(String airbnbListingId) {
            this.airbnbListingId = airbnbListingId;
            return this;
        }

        public Builder icalUrlAirbnb(String icalUrlAirbnb) {
            this.icalUrlAirbnb = icalUrlAirbnb;
            return this;
        }

        public Builder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder updatedAt(LocalDateTime updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public Builder publishedAt(LocalDateTime publishedAt) {
            this.publishedAt = publishedAt;
            return this;
        }

        public Property build() {
            return new Property(this);
        }
    }
}
