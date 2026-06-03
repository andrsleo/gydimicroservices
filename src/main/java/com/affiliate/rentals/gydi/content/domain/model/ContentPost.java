package com.affiliate.rentals.gydi.content.domain.model;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Aggregate root representing a piece of user-generated content in the GYDI Social Commerce platform.
 *
 * <p>Pure Java domain model — no Spring, JPA, or Lombok annotations.
 * All invariants are enforced in the factory method and business methods.</p>
 *
 * <p>Engagement score formula: (likes*1 + saves*3 + comments*2 + shares*5) / max(views, 1)</p>
 *
 * @author GYDI Development Team
 */
public final class ContentPost {

    private final Long id;
    private final Long creatorId;
    private Long propertyId;
    private String caption;
    private final ContentType type;
    private ContentStatus status;
    private String thumbnailUrl;
    private Integer viewCount;
    private Integer likeCount;
    private Integer saveCount;
    private Integer commentCount;
    private Integer shareCount;
    private Double engagementScore;
    private LocalDateTime publishedAt;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private ContentPost(Builder builder) {
        this.id = builder.id;
        this.creatorId = Objects.requireNonNull(builder.creatorId, "creatorId no puede ser nulo");
        this.propertyId = builder.propertyId;
        this.caption = builder.caption;
        this.type = Objects.requireNonNull(builder.type, "type no puede ser nulo");
        this.status = Objects.requireNonNullElse(builder.status, ContentStatus.DRAFT);
        this.thumbnailUrl = builder.thumbnailUrl;
        this.viewCount = Objects.requireNonNullElse(builder.viewCount, 0);
        this.likeCount = Objects.requireNonNullElse(builder.likeCount, 0);
        this.saveCount = Objects.requireNonNullElse(builder.saveCount, 0);
        this.commentCount = Objects.requireNonNullElse(builder.commentCount, 0);
        this.shareCount = Objects.requireNonNullElse(builder.shareCount, 0);
        this.engagementScore = Objects.requireNonNullElse(builder.engagementScore, 0.0);
        this.publishedAt = builder.publishedAt;
        this.createdAt = Objects.requireNonNullElseGet(builder.createdAt, LocalDateTime::now);
        this.updatedAt = Objects.requireNonNullElseGet(builder.updatedAt, LocalDateTime::now);
    }

    /**
     * Factory method for creating a new draft content post.
     *
     * @param creatorId  the ID of the user creating this post
     * @param caption    the sanitized caption text (max 500 chars)
     * @param type       the content type
     * @param propertyId optional linked property ID
     * @return a new ContentPost in DRAFT status
     */
    public static ContentPost create(Long creatorId, String caption, ContentType type, Long propertyId) {
        ContentPost post = new Builder()
                .creatorId(creatorId)
                .caption(caption)
                .type(type)
                .propertyId(propertyId)
                .status(ContentStatus.DRAFT)
                .viewCount(0)
                .likeCount(0)
                .saveCount(0)
                .commentCount(0)
                .shareCount(0)
                .engagementScore(0.0)
                .build();
        post.validate();
        return post;
    }

    /**
     * Transitions this post to PUBLISHED status.
     *
     * @throws IllegalStateException if the post is not in DRAFT or PROCESSING status
     */
    public void publish() {
        if (this.status != ContentStatus.DRAFT && this.status != ContentStatus.PROCESSING) {
            throw new IllegalStateException(
                    "Solo se pueden publicar posts en estado DRAFT o PROCESSING. Estado actual: " + this.status);
        }
        this.status = ContentStatus.PUBLISHED;
        this.publishedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Archives this post, making it no longer visible in feeds.
     */
    public void archive() {
        this.status = ContentStatus.ARCHIVED;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Hides this post (e.g. auto-triggered by report threshold).
     */
    public void hide() {
        this.status = ContentStatus.HIDDEN;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Rejects this post (ADMIN action).
     */
    public void reject() {
        this.status = ContentStatus.REJECTED;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Updates the caption and/or linked property.
     *
     * @param caption    new caption text (sanitized)
     * @param propertyId new linked property ID (may be null)
     */
    public void update(String caption, Long propertyId) {
        this.caption = caption;
        this.propertyId = propertyId;
        this.updatedAt = LocalDateTime.now();
    }

    /** Increments like count and recalculates engagement score. */
    public void incrementLikeCount() {
        this.likeCount++;
        recalculateEngagementScore();
        this.updatedAt = LocalDateTime.now();
    }

    /** Decrements like count and recalculates engagement score. */
    public void decrementLikeCount() {
        if (this.likeCount > 0) {
            this.likeCount--;
        }
        recalculateEngagementScore();
        this.updatedAt = LocalDateTime.now();
    }

    /** Increments save count and recalculates engagement score. */
    public void incrementSaveCount() {
        this.saveCount++;
        recalculateEngagementScore();
        this.updatedAt = LocalDateTime.now();
    }

    /** Decrements save count and recalculates engagement score. */
    public void decrementSaveCount() {
        if (this.saveCount > 0) {
            this.saveCount--;
        }
        recalculateEngagementScore();
        this.updatedAt = LocalDateTime.now();
    }

    /** Increments comment count and recalculates engagement score. */
    public void incrementCommentCount() {
        this.commentCount++;
        recalculateEngagementScore();
        this.updatedAt = LocalDateTime.now();
    }

    /** Decrements comment count and recalculates engagement score. */
    public void decrementCommentCount() {
        if (this.commentCount > 0) {
            this.commentCount--;
        }
        recalculateEngagementScore();
        this.updatedAt = LocalDateTime.now();
    }

    /** Increments share count and recalculates engagement score. */
    public void incrementShareCount() {
        this.shareCount++;
        recalculateEngagementScore();
        this.updatedAt = LocalDateTime.now();
    }

    /** Sets the thumbnail URL. */
    public void setThumbnailUrl(String thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
        this.updatedAt = LocalDateTime.now();
    }

    private void recalculateEngagementScore() {
        int views = Math.max(this.viewCount, 1);
        this.engagementScore = (this.likeCount * 1.0
                + this.saveCount * 3.0
                + this.commentCount * 2.0
                + this.shareCount * 5.0) / views;
    }

    private void validate() {
        if (caption != null && caption.length() > 500) {
            throw new IllegalArgumentException("El caption no puede superar 500 caracteres");
        }
    }

    public Long id() { return id; }
    public Long creatorId() { return creatorId; }
    public Long propertyId() { return propertyId; }
    public String caption() { return caption; }
    public ContentType type() { return type; }
    public ContentStatus status() { return status; }
    public String thumbnailUrl() { return thumbnailUrl; }
    public Integer viewCount() { return viewCount; }
    public Integer likeCount() { return likeCount; }
    public Integer saveCount() { return saveCount; }
    public Integer commentCount() { return commentCount; }
    public Integer shareCount() { return shareCount; }
    public Double engagementScore() { return engagementScore; }
    public LocalDateTime publishedAt() { return publishedAt; }
    public LocalDateTime createdAt() { return createdAt; }
    public LocalDateTime updatedAt() { return updatedAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ContentPost that = (ContentPost) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "ContentPost[id=%s, creatorId=%s, type=%s, status=%s]"
                .formatted(id, creatorId, type, status);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private Long id;
        private Long creatorId;
        private Long propertyId;
        private String caption;
        private ContentType type;
        private ContentStatus status;
        private String thumbnailUrl;
        private Integer viewCount;
        private Integer likeCount;
        private Integer saveCount;
        private Integer commentCount;
        private Integer shareCount;
        private Double engagementScore;
        private LocalDateTime publishedAt;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        private Builder() {}

        public Builder id(Long id) { this.id = id; return this; }
        public Builder creatorId(Long creatorId) { this.creatorId = creatorId; return this; }
        public Builder propertyId(Long propertyId) { this.propertyId = propertyId; return this; }
        public Builder caption(String caption) { this.caption = caption; return this; }
        public Builder type(ContentType type) { this.type = type; return this; }
        public Builder status(ContentStatus status) { this.status = status; return this; }
        public Builder thumbnailUrl(String thumbnailUrl) { this.thumbnailUrl = thumbnailUrl; return this; }
        public Builder viewCount(Integer viewCount) { this.viewCount = viewCount; return this; }
        public Builder likeCount(Integer likeCount) { this.likeCount = likeCount; return this; }
        public Builder saveCount(Integer saveCount) { this.saveCount = saveCount; return this; }
        public Builder commentCount(Integer commentCount) { this.commentCount = commentCount; return this; }
        public Builder shareCount(Integer shareCount) { this.shareCount = shareCount; return this; }
        public Builder engagementScore(Double engagementScore) { this.engagementScore = engagementScore; return this; }
        public Builder publishedAt(LocalDateTime publishedAt) { this.publishedAt = publishedAt; return this; }
        public Builder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public Builder updatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; return this; }

        public ContentPost build() {
            return new ContentPost(this);
        }
    }
}
