package com.affiliate.rentals.gydi.users.domain.model;

import java.time.LocalDateTime;
import java.util.Objects;

public final class CreatorProfile {

    private final Long userId;
    private Integer contentCount;
    private Integer followerCount;
    private Integer followingCount;
    private Long totalViews;
    private Double avgEngagementRate;
    private CreatorTier tier;
    private LocalDateTime lastContentAt;
    private LocalDateTime updatedAt;

    private CreatorProfile(Long userId, Integer contentCount, Integer followerCount,
                           Integer followingCount, Long totalViews, Double avgEngagementRate,
                           CreatorTier tier, LocalDateTime lastContentAt, LocalDateTime updatedAt) {
        this.userId = Objects.requireNonNull(userId, "userId requerido");
        this.contentCount = contentCount != null ? contentCount : 0;
        this.followerCount = followerCount != null ? followerCount : 0;
        this.followingCount = followingCount != null ? followingCount : 0;
        this.totalViews = totalViews != null ? totalViews : 0L;
        this.avgEngagementRate = avgEngagementRate != null ? avgEngagementRate : 0.0;
        this.tier = tier != null ? tier : CreatorTier.EMERGING;
        this.lastContentAt = lastContentAt;
        this.updatedAt = updatedAt != null ? updatedAt : LocalDateTime.now();
    }

    public static CreatorProfile create(Long userId) {
        return new CreatorProfile(userId, 0, 0, 0, 0L, 0.0, CreatorTier.EMERGING,
                null, LocalDateTime.now());
    }

    public static CreatorProfile reconstitute(Long userId, Integer contentCount, Integer followerCount,
                                              Integer followingCount, Long totalViews,
                                              Double avgEngagementRate, CreatorTier tier,
                                              LocalDateTime lastContentAt, LocalDateTime updatedAt) {
        return new CreatorProfile(userId, contentCount, followerCount, followingCount,
                totalViews, avgEngagementRate, tier, lastContentAt, updatedAt);
    }

    public Long userId() { return userId; }
    public Integer contentCount() { return contentCount; }
    public Integer followerCount() { return followerCount; }
    public Integer followingCount() { return followingCount; }
    public Long totalViews() { return totalViews; }
    public Double avgEngagementRate() { return avgEngagementRate; }
    public CreatorTier tier() { return tier; }
    public LocalDateTime lastContentAt() { return lastContentAt; }
    public LocalDateTime updatedAt() { return updatedAt; }
}
