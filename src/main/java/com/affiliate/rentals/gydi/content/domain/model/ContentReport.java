package com.affiliate.rentals.gydi.content.domain.model;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Domain model representing a user report against a content post.
 *
 * <p>Pure Java — no Spring, JPA, or Lombok annotations.</p>
 *
 * @author GYDI Development Team
 */
public final class ContentReport {

    private final Long id;
    private final Long contentPostId;
    private final Long reporterId;
    private final ReportReason reason;
    private final String description;
    private ReportStatus status;
    private Long reviewedBy;
    private final LocalDateTime createdAt;
    private LocalDateTime reviewedAt;

    private ContentReport(Builder builder) {
        this.id = builder.id;
        this.contentPostId = Objects.requireNonNull(builder.contentPostId, "contentPostId no puede ser nulo");
        this.reporterId = Objects.requireNonNull(builder.reporterId, "reporterId no puede ser nulo");
        this.reason = Objects.requireNonNull(builder.reason, "reason no puede ser nulo");
        this.description = builder.description;
        this.status = Objects.requireNonNullElse(builder.status, ReportStatus.PENDING);
        this.reviewedBy = builder.reviewedBy;
        this.createdAt = Objects.requireNonNullElseGet(builder.createdAt, LocalDateTime::now);
        this.reviewedAt = builder.reviewedAt;
    }

    /**
     * Factory method to create a new content report.
     */
    public static ContentReport create(Long contentPostId, Long reporterId, ReportReason reason, String description) {
        return new Builder()
                .contentPostId(contentPostId)
                .reporterId(reporterId)
                .reason(reason)
                .description(description)
                .status(ReportStatus.PENDING)
                .build();
    }

    /**
     * Marks the report as reviewed by an admin.
     *
     * @param adminId  the ID of the reviewing admin
     * @param newStatus the new status after review
     */
    public void review(Long adminId, ReportStatus newStatus) {
        this.reviewedBy = adminId;
        this.status = newStatus;
        this.reviewedAt = LocalDateTime.now();
    }

    public Long id() { return id; }
    public Long contentPostId() { return contentPostId; }
    public Long reporterId() { return reporterId; }
    public ReportReason reason() { return reason; }
    public String description() { return description; }
    public ReportStatus status() { return status; }
    public Long reviewedBy() { return reviewedBy; }
    public LocalDateTime createdAt() { return createdAt; }
    public LocalDateTime reviewedAt() { return reviewedAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ContentReport that = (ContentReport) o;
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
        private Long reporterId;
        private ReportReason reason;
        private String description;
        private ReportStatus status;
        private Long reviewedBy;
        private LocalDateTime createdAt;
        private LocalDateTime reviewedAt;

        private Builder() {}

        public Builder id(Long id) { this.id = id; return this; }
        public Builder contentPostId(Long contentPostId) { this.contentPostId = contentPostId; return this; }
        public Builder reporterId(Long reporterId) { this.reporterId = reporterId; return this; }
        public Builder reason(ReportReason reason) { this.reason = reason; return this; }
        public Builder description(String description) { this.description = description; return this; }
        public Builder status(ReportStatus status) { this.status = status; return this; }
        public Builder reviewedBy(Long reviewedBy) { this.reviewedBy = reviewedBy; return this; }
        public Builder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public Builder reviewedAt(LocalDateTime reviewedAt) { this.reviewedAt = reviewedAt; return this; }

        public ContentReport build() {
            return new ContentReport(this);
        }
    }
}
