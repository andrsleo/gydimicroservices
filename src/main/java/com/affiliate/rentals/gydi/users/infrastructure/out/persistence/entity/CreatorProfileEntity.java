package com.affiliate.rentals.gydi.users.infrastructure.out.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "creator_profiles", schema = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreatorProfileEntity {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "content_count", nullable = false)
    private Integer contentCount = 0;

    @Column(name = "follower_count", nullable = false)
    private Integer followerCount = 0;

    @Column(name = "following_count", nullable = false)
    private Integer followingCount = 0;

    @Column(name = "total_views", nullable = false)
    private Long totalViews = 0L;

    @Column(name = "avg_engagement_rate", nullable = false)
    private Double avgEngagementRate = 0.0;

    @Column(name = "tier", nullable = false, length = 20)
    private String tier = "EMERGING";

    @Column(name = "last_content_at")
    private LocalDateTime lastContentAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (updatedAt == null) updatedAt = LocalDateTime.now();
        if (tier == null) tier = "EMERGING";
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
