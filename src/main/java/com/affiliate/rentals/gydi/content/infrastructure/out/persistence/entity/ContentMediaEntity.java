package com.affiliate.rentals.gydi.content.infrastructure.out.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "content_media", schema = "content")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ContentMediaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "content_post_id", nullable = false)
    private Long contentPostId;

    @Column(name = "original_url", nullable = false)
    private String originalUrl;

    @Column(name = "processed_url")
    private String processedUrl;

    @Column(name = "thumbnail_url")
    private String thumbnailUrl;

    @Column(name = "media_type", nullable = false, length = 10)
    private String mediaType;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder = 0;

    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    @Column(name = "width")
    private Integer width;

    @Column(name = "height")
    private Integer height;

    @Column(name = "file_size_bytes")
    private Long fileSizeBytes;

    @Column(name = "processing_status", nullable = false, length = 20)
    private String processingStatus = "PENDING";

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (processingStatus == null) processingStatus = "PENDING";
    }
}
