package com.affiliate.rentals.gydi.content.domain.model;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Domain model representing a media file (image or video) attached to a ContentPost.
 *
 * <p>Pure Java — no Spring, JPA, or Lombok annotations.</p>
 *
 * @author GYDI Development Team
 */
public final class ContentMedia {

    private final Long id;
    private final Long contentPostId;
    private final String originalUrl;
    private String processedUrl;
    private String thumbnailUrl;
    private final MediaType mediaType;
    private final Integer displayOrder;
    private final Integer durationSeconds;
    private final Integer width;
    private final Integer height;
    private final Long fileSizeBytes;
    private ProcessingStatus processingStatus;
    private final LocalDateTime createdAt;

    private ContentMedia(Builder builder) {
        this.id = builder.id;
        this.contentPostId = Objects.requireNonNull(builder.contentPostId, "contentPostId no puede ser nulo");
        this.originalUrl = Objects.requireNonNull(builder.originalUrl, "originalUrl no puede ser nulo");
        this.processedUrl = builder.processedUrl;
        this.thumbnailUrl = builder.thumbnailUrl;
        this.mediaType = Objects.requireNonNull(builder.mediaType, "mediaType no puede ser nulo");
        this.displayOrder = Objects.requireNonNullElse(builder.displayOrder, 0);
        this.durationSeconds = builder.durationSeconds;
        this.width = builder.width;
        this.height = builder.height;
        this.fileSizeBytes = builder.fileSizeBytes;
        this.processingStatus = Objects.requireNonNullElse(builder.processingStatus, ProcessingStatus.PENDING);
        this.createdAt = Objects.requireNonNullElseGet(builder.createdAt, LocalDateTime::now);
    }

    public void markReady(String processedUrl, String thumbnailUrl) {
        this.processedUrl = processedUrl;
        this.thumbnailUrl = thumbnailUrl;
        this.processingStatus = ProcessingStatus.READY;
    }

    public void markFailed() {
        this.processingStatus = ProcessingStatus.FAILED;
    }

    public Long id() { return id; }
    public Long contentPostId() { return contentPostId; }
    public String originalUrl() { return originalUrl; }
    public String processedUrl() { return processedUrl; }
    public String thumbnailUrl() { return thumbnailUrl; }
    public MediaType mediaType() { return mediaType; }
    public Integer displayOrder() { return displayOrder; }
    public Integer durationSeconds() { return durationSeconds; }
    public Integer width() { return width; }
    public Integer height() { return height; }
    public Long fileSizeBytes() { return fileSizeBytes; }
    public ProcessingStatus processingStatus() { return processingStatus; }
    public LocalDateTime createdAt() { return createdAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ContentMedia that = (ContentMedia) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private Long id;
        private Long contentPostId;
        private String originalUrl;
        private String processedUrl;
        private String thumbnailUrl;
        private MediaType mediaType;
        private Integer displayOrder;
        private Integer durationSeconds;
        private Integer width;
        private Integer height;
        private Long fileSizeBytes;
        private ProcessingStatus processingStatus;
        private LocalDateTime createdAt;

        private Builder() {}

        public Builder id(Long id) { this.id = id; return this; }
        public Builder contentPostId(Long contentPostId) { this.contentPostId = contentPostId; return this; }
        public Builder originalUrl(String originalUrl) { this.originalUrl = originalUrl; return this; }
        public Builder processedUrl(String processedUrl) { this.processedUrl = processedUrl; return this; }
        public Builder thumbnailUrl(String thumbnailUrl) { this.thumbnailUrl = thumbnailUrl; return this; }
        public Builder mediaType(MediaType mediaType) { this.mediaType = mediaType; return this; }
        public Builder displayOrder(Integer displayOrder) { this.displayOrder = displayOrder; return this; }
        public Builder durationSeconds(Integer durationSeconds) { this.durationSeconds = durationSeconds; return this; }
        public Builder width(Integer width) { this.width = width; return this; }
        public Builder height(Integer height) { this.height = height; return this; }
        public Builder fileSizeBytes(Long fileSizeBytes) { this.fileSizeBytes = fileSizeBytes; return this; }
        public Builder processingStatus(ProcessingStatus processingStatus) { this.processingStatus = processingStatus; return this; }
        public Builder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }

        public ContentMedia build() {
            return new ContentMedia(this);
        }
    }
}
