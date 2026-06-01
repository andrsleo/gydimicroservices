package com.affiliate.rentals.gydi.collaborations.domain.model;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.Set;

/**
 * Value object representing a file uploaded by a creator for a deliverable.
 * Pure Java — no framework annotations.
 */
public final class DeliveryAsset {

    private static final Set<String> ALLOWED_TYPES = Set.of("mp4", "jpg", "png", "zip");
    private static final long MAX_VIDEO_ZIP_BYTES = 100L * 1024 * 1024;  // 100 MB
    private static final long MAX_IMAGE_BYTES = 10L * 1024 * 1024;       // 10 MB

    private final Long id;
    private final Long agreementId;
    private final Long agreementDeliverableId;
    private final String fileUrl;
    private final String fileType;
    private final long fileSizeBytes;
    private String reviewStatus;
    private final OffsetDateTime uploadedAt;

    private DeliveryAsset(Long id, Long agreementId, Long agreementDeliverableId,
                          String fileUrl, String fileType, long fileSizeBytes,
                          String reviewStatus, OffsetDateTime uploadedAt) {
        this.id = id;
        this.agreementId = Objects.requireNonNull(agreementId, "agreementId must not be null");
        this.agreementDeliverableId = Objects.requireNonNull(agreementDeliverableId, "agreementDeliverableId must not be null");
        this.fileUrl = Objects.requireNonNull(fileUrl, "fileUrl must not be null");
        String normalizedType = Objects.requireNonNull(fileType, "fileType must not be null").toLowerCase();
        if (!ALLOWED_TYPES.contains(normalizedType))
            throw new IllegalArgumentException("Unsupported file type: " + fileType);
        this.fileType = normalizedType;
        validateFileSize(normalizedType, fileSizeBytes);
        this.fileSizeBytes = fileSizeBytes;
        this.reviewStatus = Objects.requireNonNullElse(reviewStatus, "PENDING");
        this.uploadedAt = Objects.requireNonNullElseGet(uploadedAt, OffsetDateTime::now);
    }

    public static DeliveryAsset create(Long agreementId, Long agreementDeliverableId,
                                       String fileUrl, String fileType, long fileSizeBytes) {
        return new DeliveryAsset(null, agreementId, agreementDeliverableId,
                fileUrl, fileType, fileSizeBytes, "PENDING", OffsetDateTime.now());
    }

    public static DeliveryAsset reconstitute(Long id, Long agreementId, Long agreementDeliverableId,
                                             String fileUrl, String fileType, long fileSizeBytes,
                                             String reviewStatus, OffsetDateTime uploadedAt) {
        return new DeliveryAsset(id, agreementId, agreementDeliverableId,
                fileUrl, fileType, fileSizeBytes, reviewStatus, uploadedAt);
    }

    private static void validateFileSize(String fileType, long bytes) {
        if (fileType.equals("jpg") || fileType.equals("png")) {
            if (bytes > MAX_IMAGE_BYTES)
                throw new IllegalArgumentException("Image files must be <= 10 MB");
        } else {
            if (bytes > MAX_VIDEO_ZIP_BYTES)
                throw new IllegalArgumentException("Video/zip files must be <= 100 MB");
        }
    }

    public Long id() { return id; }
    public Long agreementId() { return agreementId; }
    public Long agreementDeliverableId() { return agreementDeliverableId; }
    public String fileUrl() { return fileUrl; }
    public String fileType() { return fileType; }
    public long fileSizeBytes() { return fileSizeBytes; }
    public String reviewStatus() { return reviewStatus; }
    public OffsetDateTime uploadedAt() { return uploadedAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DeliveryAsset that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
