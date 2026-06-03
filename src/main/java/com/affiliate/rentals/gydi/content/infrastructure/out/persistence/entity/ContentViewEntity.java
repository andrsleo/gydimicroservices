package com.affiliate.rentals.gydi.content.infrastructure.out.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "content_views", schema = "content")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ContentViewEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "content_post_id", nullable = false)
    private Long contentPostId;

    @Column(name = "viewer_id")
    private Long viewerId;

    @Column(name = "viewer_ip", length = 45)
    private String viewerIp;

    @Column(name = "watch_duration_seconds")
    private Integer watchDurationSeconds;

    @Column(name = "source", length = 20)
    private String source;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
