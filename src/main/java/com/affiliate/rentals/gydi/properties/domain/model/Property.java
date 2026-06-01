package com.affiliate.rentals.gydi.properties.domain.model;

import com.affiliate.rentals.gydi.properties.domain.exception.PropertyCannotBePublishedException;
import com.affiliate.rentals.gydi.properties.domain.exception.PropertyTransitionNotAllowedException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Property aggregate root representing a vacation rental property.
 * Enforces business rules and manages PropertyImage and PropertyVideo entities.
 *
 * State machine:
 *   DRAFT -> PENDING_APPROVAL  (auto, when minimums met on save or image upload/delete)
 *   PENDING_APPROVAL -> PUBLISHED  (ADMIN approves)
 *   PENDING_APPROVAL -> DENY       (ADMIN rejects)
 *   DENY -> PENDING_APPROVAL       (host re-submits after edits)
 *   PUBLISHED -> INACTIVE          (host deactivates or ADMIN)
 *   INACTIVE -> PUBLISHED          (host reactivates or ADMIN)
 */
public class Property {
    private static final int MAX_IMAGES = 20;
    private static final int MAX_VIDEOS = 2;
    private static final int MIN_IMAGES_FOR_PUBLISH = 4;

    private final PropertyId id;
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
    private final List<PropertyImage> images;
    private final List<PropertyVideo> videos;
    private Long coverImageId;
    private String airbnbUrl;
    private ImportMode importMode;
    private LocalDateTime importedAt;
    private String airbnbListingId;
    private String icalUrlAirbnb;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime publishedAt;
    private String denialReason;
    private LocalDateTime submittedAt;
    private LocalDateTime approvedAt;
    private LocalDateTime deniedAt;
    private boolean acceptCreatorCollaborations;
    private List<String> acceptedCompensations;

    private Property(Builder builder) {
        this.id = builder.id;
        this.hostId = Objects.requireNonNull(builder.hostId, "Host ID cannot be null");
        this.title = Objects.requireNonNull(builder.title, "Title cannot be null");
        this.slug = builder.slug;
        this.description = builder.description;
        this.pricePerNight = Objects.requireNonNull(builder.pricePerNight, "Price per night cannot be null");
        this.salePrice = builder.salePrice;
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
        this.denialReason = builder.denialReason;
        this.submittedAt = builder.submittedAt;
        this.approvedAt = builder.approvedAt;
        this.deniedAt = builder.deniedAt;
        this.acceptCreatorCollaborations = builder.acceptCreatorCollaborations;
        this.acceptedCompensations = builder.acceptedCompensations != null
                ? new ArrayList<>(builder.acceptedCompensations) : new ArrayList<>();
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

    // ── Status transition methods ──────────────────────────────────────────────

    /**
     * Publishes this property directly (legacy/admin path).
     * Validates all required fields before publishing.
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

    public boolean canBePublished() {
        return collectPublishValidationErrors().isEmpty();
    }

    /**
     * Checks if this property meets the minimum content requirements to be submitted for approval.
     * Used by the update use case and media use cases to decide whether to auto-transition
     * DRAFT -> PENDING_APPROVAL.
     */
    public boolean meetsSubmitConditions() {
        return collectSubmitValidationErrors().isEmpty();
    }

    /**
     * Auto-transitions a DRAFT property to PENDING_APPROVAL when content minimums are met.
     * Called by the update use case and image upload/delete use cases after saving.
     * No-op if the property is not in DRAFT status.
     */
    public void autoTransitionIfReady() {
        if (this.status == PropertyStatus.DRAFT && meetsSubmitConditions()) {
            this.status = PropertyStatus.PENDING_APPROVAL;
            this.submittedAt = LocalDateTime.now();
            this.denialReason = null;
            this.updatedAt = LocalDateTime.now();
        }
    }

    /**
     * Demotes a PENDING_APPROVAL property back to DRAFT when content minimums are broken
     * (e.g. an image was deleted dropping the count below the minimum).
     * No-op if the property is not in PENDING_APPROVAL status.
     */
    public void demoteToDraftIfMinimumsBroken() {
        if (this.status == PropertyStatus.PENDING_APPROVAL && !meetsSubmitConditions()) {
            this.status = PropertyStatus.DRAFT;
            this.submittedAt = null;
            this.updatedAt = LocalDateTime.now();
        }
    }

    /**
     * Submits a DENY property for re-review after the host has made corrections.
     * Only allowed from DENY status. For first-time submission from DRAFT, the system
     * calls autoTransitionIfReady() automatically on save.
     *
     * @throws PropertyTransitionNotAllowedException if current status is not DENY
     * @throws PropertyCannotBePublishedException    if content validation fails
     */
    public void submitForApproval() {
        if (this.status != PropertyStatus.DENY) {
            throw new PropertyTransitionNotAllowedException(this.status, PropertyStatus.PENDING_APPROVAL);
        }
        List<String> errors = collectSubmitValidationErrors();
        if (!errors.isEmpty()) {
            throw new PropertyCannotBePublishedException(errors);
        }
        this.status = PropertyStatus.PENDING_APPROVAL;
        this.submittedAt = LocalDateTime.now();
        this.denialReason = null;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Approves a pending property, making it publicly visible.
     * Only allowed when status is PENDING_APPROVAL.
     */
    public void approvePendingProperty() {
        if (this.status != PropertyStatus.PENDING_APPROVAL) {
            throw new PropertyTransitionNotAllowedException(this.status, PropertyStatus.PUBLISHED);
        }
        this.status = PropertyStatus.PUBLISHED;
        this.approvedAt = LocalDateTime.now();
        this.publishedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Denies a pending property with a reason. Host can fix and re-submit.
     * Only allowed when status is PENDING_APPROVAL.
     */
    public void denyProperty(String reason) {
        if (this.status != PropertyStatus.PENDING_APPROVAL) {
            throw new PropertyTransitionNotAllowedException(this.status, PropertyStatus.DENY);
        }
        this.status = PropertyStatus.DENY;
        this.denialReason = reason;
        this.deniedAt = LocalDateTime.now();
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

    /**
     * Reactivates a property that was previously approved and is now inactive.
     * Only allowed when status is INACTIVE.
     */
    public void reactivate() {
        if (this.status != PropertyStatus.INACTIVE) {
            throw new PropertyTransitionNotAllowedException(this.status, PropertyStatus.PUBLISHED);
        }
        this.status = PropertyStatus.PUBLISHED;
        this.updatedAt = LocalDateTime.now();
    }

    public void delete() {
        this.status = PropertyStatus.DELETED;
        this.updatedAt = LocalDateTime.now();
    }

    // ── Validation helpers ─────────────────────────────────────────────────────

    private List<String> collectPublishValidationErrors() {
        List<String> errors = new ArrayList<>();
        validateImages(errors);
        validateBasicInfo(errors);
        validatePricing(errors);
        validateLocation(errors);
        validateSpecs(errors);
        validateAmenities(errors);
        validateListingType(errors);
        validateStatusForPublish(errors);
        return errors;
    }

    private List<String> collectSubmitValidationErrors() {
        List<String> errors = new ArrayList<>();
        validateImages(errors);
        validateBasicInfo(errors);
        validatePricing(errors);
        validateLocation(errors);
        validateSpecs(errors);
        validateAmenities(errors);
        validateListingType(errors);
        return errors;
    }

    private void validateImages(List<String> errors) {
        if (images.size() < MIN_IMAGES_FOR_PUBLISH) {
            errors.add(String.format("Property must have at least %d images (currently has %d)",
                    MIN_IMAGES_FOR_PUBLISH, images.size()));
        }
    }

    private void validateBasicInfo(List<String> errors) {
        if (title == null || title.isBlank()) errors.add("Title is required");
        if (description == null || description.isBlank()) errors.add("Description is required");
    }

    private void validatePricing(List<String> errors) {
        if (pricePerNight == null) errors.add("Price per night is required");
        if (propertyType == null) errors.add("Property type is required");
    }

    private void validateLocation(List<String> errors) {
        if (location == null) { errors.add("Location is required"); return; }
        if (location.country() == null || location.country().isBlank()) errors.add("Country is required");
        if (location.city() == null || location.city().isBlank()) errors.add("City is required");
        if (location.address() == null || location.address().isBlank()) errors.add("Address is required");
        if (location.postalCode() == null || location.postalCode().isBlank()) errors.add("Postal code is required");
    }

    private void validateSpecs(List<String> errors) {
        if (specs == null) { errors.add("Property specifications are required"); return; }
        if (specs.bedrooms() <= 0) errors.add("Property must have at least 1 bedroom");
        if (specs.bathrooms() <= 0) errors.add("Property must have at least 1 bathroom");
        if (specs.maxGuests() <= 0) errors.add("Property must accommodate at least 1 guest");
    }

    private void validateAmenities(List<String> errors) {
        if (amenities == null || amenities.isEmpty()) errors.add("Property must have at least 1 amenity");
    }

    private void validateListingType(List<String> errors) {
        if (listingType == null) { errors.add("Listing type is required"); return; }
        if (listingType.allowsSale()
                && (salePrice == null || salePrice.amount().compareTo(java.math.BigDecimal.ZERO) <= 0)) {
            errors.add("Sale price is required for properties available for sale");
        }
        if (listingType.allowsRental()
                && (pricePerNight == null || pricePerNight.amount().compareTo(java.math.BigDecimal.ZERO) <= 0)) {
            errors.add("Price per night is required for rental properties");
        }
        if (listingType.allowsRental() && (specs == null || specs.maxGuests() <= 0)) {
            errors.add("Max guests is required for rental properties");
        }
    }

    private void validateStatusForPublish(List<String> errors) {
        if (status != PropertyStatus.DRAFT && status != PropertyStatus.DENY
                && status != PropertyStatus.PENDING_APPROVAL
                && status != PropertyStatus.PUBLISHED && status != PropertyStatus.INACTIVE) {
            errors.add("Property cannot be submitted for approval from status: " + status);
        }
    }

    // ── Media management ───────────────────────────────────────────────────────

    public void addImage(String url, int displayOrder) {
        if (images.size() >= MAX_IMAGES) {
            throw new IllegalStateException("Maximum number of images (" + MAX_IMAGES + ") reached");
        }
        int finalDisplayOrder;
        if (displayOrder == -1) {
            finalDisplayOrder = images.stream().mapToInt(PropertyImage::getDisplayOrder).max().orElse(-1) + 1;
        } else {
            boolean orderExists = images.stream().anyMatch(img -> img.getDisplayOrder() == displayOrder);
            finalDisplayOrder = orderExists
                    ? images.stream().mapToInt(PropertyImage::getDisplayOrder).max().orElse(-1) + 1
                    : displayOrder;
        }
        this.images.add(PropertyImage.create(this.id, url, finalDisplayOrder));
        this.updatedAt = LocalDateTime.now();
    }

    public void addVideo(String url, int displayOrder) {
        if (videos.size() >= MAX_VIDEOS) {
            throw new IllegalStateException("Maximum number of videos (" + MAX_VIDEOS + ") reached");
        }
        int finalDisplayOrder;
        if (displayOrder == -1) {
            finalDisplayOrder = videos.stream().mapToInt(PropertyVideo::getDisplayOrder).max().orElse(-1) + 1;
        } else {
            boolean orderExists = videos.stream().anyMatch(vid -> vid.getDisplayOrder() == displayOrder);
            finalDisplayOrder = orderExists
                    ? videos.stream().mapToInt(PropertyVideo::getDisplayOrder).max().orElse(-1) + 1
                    : displayOrder;
        }
        this.videos.add(PropertyVideo.create(this.id, url, finalDisplayOrder));
        this.updatedAt = LocalDateTime.now();
    }

    public void addVideo(String url, String thumbnailUrl, int displayOrder, Integer durationSeconds) {
        if (videos.size() >= MAX_VIDEOS) {
            throw new IllegalStateException("Maximum number of videos (" + MAX_VIDEOS + ") reached");
        }
        int finalDisplayOrder;
        if (displayOrder == -1) {
            finalDisplayOrder = videos.stream().mapToInt(PropertyVideo::getDisplayOrder).max().orElse(-1) + 1;
        } else {
            boolean orderExists = videos.stream().anyMatch(vid -> vid.getDisplayOrder() == displayOrder);
            finalDisplayOrder = orderExists
                    ? videos.stream().mapToInt(PropertyVideo::getDisplayOrder).max().orElse(-1) + 1
                    : displayOrder;
        }
        this.videos.add(PropertyVideo.create(this.id, url, thumbnailUrl, finalDisplayOrder, durationSeconds));
        this.updatedAt = LocalDateTime.now();
    }

    public void removeImage(Long imageId) {
        if (images.removeIf(img -> img.getId().equals(imageId))) {
            this.updatedAt = LocalDateTime.now();
        }
    }

    public void removeVideo(Long videoId) {
        if (videos.removeIf(vid -> vid.getId().equals(videoId))) {
            this.updatedAt = LocalDateTime.now();
        }
    }

    public void reorderImages(java.util.Map<Long, Integer> imageOrders) {
        if (imageOrders.isEmpty()) throw new IllegalArgumentException("Cannot reorder with empty orders");
        java.util.Set<Long> currentIds = images.stream()
                .map(PropertyImage::getId).collect(java.util.stream.Collectors.toSet());
        if (!imageOrders.keySet().equals(currentIds)) {
            throw new IllegalArgumentException("Image IDs in reorder request do not match current images");
        }
        long distinctOrders = imageOrders.values().stream().distinct().count();
        if (distinctOrders != imageOrders.size()) throw new IllegalArgumentException("Duplicate display orders detected");
        for (PropertyImage image : this.images) image.setDisplayOrder(imageOrders.get(image.getId()));
        this.images.sort(java.util.Comparator.comparingInt(PropertyImage::getDisplayOrder));
        this.updatedAt = LocalDateTime.now();
    }

    public void reorderVideos(java.util.Map<Long, Integer> videoOrders) {
        if (videoOrders.isEmpty()) throw new IllegalArgumentException("Cannot reorder with empty orders");
        java.util.Set<Long> currentIds = videos.stream()
                .map(PropertyVideo::getId).collect(java.util.stream.Collectors.toSet());
        if (!videoOrders.keySet().equals(currentIds)) {
            throw new IllegalArgumentException("Video IDs in reorder request do not match current videos");
        }
        long distinctOrders = videoOrders.values().stream().distinct().count();
        if (distinctOrders != videoOrders.size()) throw new IllegalArgumentException("Duplicate display orders detected");
        for (PropertyVideo video : this.videos) video.setDisplayOrder(videoOrders.get(video.getId()));
        this.videos.sort(java.util.Comparator.comparingInt(PropertyVideo::getDisplayOrder));
        this.updatedAt = LocalDateTime.now();
    }

    public void setCoverImage(Long imageId) {
        if (imageId == null) { this.coverImageId = null; this.updatedAt = LocalDateTime.now(); return; }
        if (images.isEmpty()) throw new IllegalStateException("Cannot set cover image when property has no images");
        if (images.stream().noneMatch(img -> img.getId().equals(imageId))) {
            throw new IllegalArgumentException("Image not found in property images");
        }
        this.coverImageId = imageId;
        this.updatedAt = LocalDateTime.now();
    }

    public PropertyImage getCoverImage() {
        if (images.isEmpty()) return null;
        if (coverImageId != null) {
            return images.stream().filter(img -> img.getId().equals(coverImageId)).findFirst().orElse(null);
        }
        return images.stream().min(java.util.Comparator.comparingInt(PropertyImage::getDisplayOrder)).orElse(null);
    }

    public Long getCoverImageId() { return coverImageId; }

    // ── Detail update methods ──────────────────────────────────────────────────

    public void updateDetails(String title, String description, Money pricePerNight, Money salePrice) {
        if (title != null) {
            if (title.isBlank() || title.length() < 10 || title.length() > 100)
                throw new IllegalArgumentException("Invalid title");
            this.title = title;
        }
        if (description != null) {
            if (description.length() > 2000) throw new IllegalArgumentException("Description too long");
            this.description = description;
        }
        if (pricePerNight != null) this.pricePerNight = pricePerNight;
        if (salePrice != null) this.salePrice = salePrice;
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

    public void updateListingType(PropertyListingType newListingType) {
        Objects.requireNonNull(newListingType, "Listing type cannot be null");
        if (!this.listingType.canTransitionTo(newListingType)) {
            throw new IllegalStateException(
                    String.format("Cannot change listing type from %s to %s", this.listingType, newListingType));
        }
        this.listingType = newListingType;
        this.updatedAt = LocalDateTime.now();
    }

    public void addAmenity(String amenity) {
        if (!this.amenities.contains(amenity)) { this.amenities.add(amenity); this.updatedAt = LocalDateTime.now(); }
    }

    public void removeAmenity(String amenity) {
        if (this.amenities.remove(amenity)) this.updatedAt = LocalDateTime.now();
    }

    public void clearAmenities() {
        if (!this.amenities.isEmpty()) { this.amenities.clear(); this.updatedAt = LocalDateTime.now(); }
    }

    public void updatePropertyType(PropertyType newPropertyType) {
        Objects.requireNonNull(newPropertyType, "Property type cannot be null");
        this.propertyType = newPropertyType;
        this.updatedAt = LocalDateTime.now();
    }

    // ── Getters ────────────────────────────────────────────────────────────────

    public boolean isOwnedBy(Long userId) { return this.hostId.equals(userId); }
    public int getImageCount() { return images.size(); }
    public int getVideoCount() { return videos.size(); }
    public PropertyId getId() { return id; }
    public Long getHostId() { return hostId; }
    public String getTitle() { return title; }
    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; this.updatedAt = LocalDateTime.now(); }
    public void updateIcalUrlAirbnb(String icalUrlAirbnb) { this.icalUrlAirbnb = icalUrlAirbnb; this.updatedAt = LocalDateTime.now(); }
    public void updateAirbnbUrl(String airbnbUrl, String airbnbListingId) { this.airbnbUrl = airbnbUrl; this.airbnbListingId = airbnbListingId; this.updatedAt = LocalDateTime.now(); }
    public String getDescription() { return description; }
    public Money getPricePerNight() { return pricePerNight; }
    public Money getSalePrice() { return salePrice; }
    public PropertyLocation getLocation() { return location; }
    public List<String> getAmenities() { return Collections.unmodifiableList(amenities); }
    public PropertySpecs getSpecs() { return specs; }
    public PropertyType getPropertyType() { return propertyType; }
    public PropertyListingType getListingType() { return listingType; }
    public PropertyStatus getStatus() { return status; }
    public List<PropertyImage> getImages() { return Collections.unmodifiableList(images); }
    public List<PropertyVideo> getVideos() { return Collections.unmodifiableList(videos); }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public LocalDateTime getPublishedAt() { return publishedAt; }
    public String getAirbnbUrl() { return airbnbUrl; }
    public ImportMode getImportMode() { return importMode; }
    public LocalDateTime getImportedAt() { return importedAt; }
    public String getAirbnbListingId() { return airbnbListingId; }
    public String getIcalUrlAirbnb() { return icalUrlAirbnb; }
    public String getDenialReason() { return denialReason; }
    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public LocalDateTime getApprovedAt() { return approvedAt; }
    public LocalDateTime getDeniedAt() { return deniedAt; }
    public boolean isAcceptCreatorCollaborations() { return acceptCreatorCollaborations; }
    public List<String> getAcceptedCompensations() { return List.copyOf(acceptedCompensations); }

    /**
     * Update collaboration settings. Property must be PUBLISHED.
     *
     * @throws IllegalStateException if property is not in PUBLISHED status
     */
    public void updateCollaborationSettings(boolean acceptCollaborations, List<String> compensations) {
        if (this.status != PropertyStatus.PUBLISHED) {
            throw new IllegalStateException("Collaboration settings can only be changed for PUBLISHED properties");
        }
        this.acceptCreatorCollaborations = acceptCollaborations;
        this.acceptedCompensations = compensations != null ? new ArrayList<>(compensations) : new ArrayList<>();
        this.updatedAt = LocalDateTime.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        return Objects.equals(id, ((Property) o).id);
    }

    @Override
    public int hashCode() { return Objects.hash(id); }

    @Override
    public String toString() {
        return "Property[id=%s, title=%s, status=%s, images=%d, videos=%d]"
                .formatted(id, title, status, images.size(), videos.size());
    }

    // ── Builder ────────────────────────────────────────────────────────────────

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
        private String denialReason;
        private LocalDateTime submittedAt;
        private LocalDateTime approvedAt;
        private LocalDateTime deniedAt;
        private boolean acceptCreatorCollaborations;
        private List<String> acceptedCompensations;

        public Builder id(PropertyId id) { this.id = id; return this; }
        public Builder hostId(Long hostId) { this.hostId = hostId; return this; }
        public Builder title(String title) { this.title = title; return this; }
        public Builder slug(String slug) { this.slug = slug; return this; }
        public Builder description(String description) { this.description = description; return this; }
        public Builder pricePerNight(Money pricePerNight) { this.pricePerNight = pricePerNight; return this; }
        public Builder salePrice(Money salePrice) { this.salePrice = salePrice; return this; }
        public Builder location(PropertyLocation location) { this.location = location; return this; }
        public Builder amenities(List<String> amenities) { this.amenities = amenities; return this; }
        public Builder specs(PropertySpecs specs) { this.specs = specs; return this; }
        public Builder propertyType(PropertyType propertyType) { this.propertyType = propertyType; return this; }
        public Builder listingType(PropertyListingType listingType) { this.listingType = listingType; return this; }
        public Builder status(PropertyStatus status) { this.status = status; return this; }
        public Builder images(List<PropertyImage> images) { this.images = images; return this; }
        public Builder videos(List<PropertyVideo> videos) { this.videos = videos; return this; }
        public Builder coverImageId(Long coverImageId) { this.coverImageId = coverImageId; return this; }
        public Builder airbnbUrl(String airbnbUrl) { this.airbnbUrl = airbnbUrl; return this; }
        public Builder importMode(ImportMode importMode) { this.importMode = importMode; return this; }
        public Builder importedAt(LocalDateTime importedAt) { this.importedAt = importedAt; return this; }
        public Builder airbnbListingId(String airbnbListingId) { this.airbnbListingId = airbnbListingId; return this; }
        public Builder icalUrlAirbnb(String icalUrlAirbnb) { this.icalUrlAirbnb = icalUrlAirbnb; return this; }
        public Builder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public Builder updatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; return this; }
        public Builder publishedAt(LocalDateTime publishedAt) { this.publishedAt = publishedAt; return this; }
        public Builder denialReason(String denialReason) { this.denialReason = denialReason; return this; }
        public Builder submittedAt(LocalDateTime submittedAt) { this.submittedAt = submittedAt; return this; }
        public Builder approvedAt(LocalDateTime approvedAt) { this.approvedAt = approvedAt; return this; }
        public Builder deniedAt(LocalDateTime deniedAt) { this.deniedAt = deniedAt; return this; }
        public Builder acceptCreatorCollaborations(boolean acceptCreatorCollaborations) { this.acceptCreatorCollaborations = acceptCreatorCollaborations; return this; }
        public Builder acceptedCompensations(List<String> acceptedCompensations) { this.acceptedCompensations = acceptedCompensations; return this; }
        public Property build() { return new Property(this); }
    }
}
