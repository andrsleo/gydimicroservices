package com.affiliate.rentals.gydi.social.domain.model;

import java.time.LocalDateTime;
import java.util.Objects;

public final class Follow {

    private final Long id;
    private final Long followerId;
    private final Long followingId;
    private final LocalDateTime createdAt;

    private Follow(Long id, Long followerId, Long followingId, LocalDateTime createdAt) {
        this.id = id;
        this.followerId = Objects.requireNonNull(followerId, "followerId requerido");
        this.followingId = Objects.requireNonNull(followingId, "followingId requerido");
        if (followerId.equals(followingId)) throw new IllegalArgumentException("No puedes seguirte a ti mismo");
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
    }

    public static Follow create(Long followerId, Long followingId) {
        return new Follow(null, followerId, followingId, LocalDateTime.now());
    }

    public static Follow reconstitute(Long id, Long followerId, Long followingId, LocalDateTime createdAt) {
        return new Follow(id, followerId, followingId, createdAt);
    }

    public Long id() { return id; }
    public Long followerId() { return followerId; }
    public Long followingId() { return followingId; }
    public LocalDateTime createdAt() { return createdAt; }
}
