package com.affiliate.rentals.gydi.properties.infrastructure.out.persistence;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "property_videos", schema = "properties")
public class PropertyVideoJpaEntity {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "property_id", nullable = false)
    private PropertyJpaEntity property;

    @Column(nullable = false, length = 500)
    private String url;

    @Column(name = "thumbnail_url", length = 500)
    private String thumbnailUrl;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;

    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    @Column(name = "uploaded_at", nullable = false)
    private LocalDateTime uploadedAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public PropertyJpaEntity getProperty() { return property; }
    public void setProperty(PropertyJpaEntity property) { this.property = property; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public String getThumbnailUrl() { return thumbnailUrl; }
    public void setThumbnailUrl(String thumbnailUrl) { this.thumbnailUrl = thumbnailUrl; }

    public Integer getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(Integer displayOrder) { this.displayOrder = displayOrder; }

    public Integer getDurationSeconds() { return durationSeconds; }
    public void setDurationSeconds(Integer durationSeconds) { this.durationSeconds = durationSeconds; }

    public LocalDateTime getUploadedAt() { return uploadedAt; }
    public void setUploadedAt(LocalDateTime uploadedAt) { this.uploadedAt = uploadedAt; }
}
