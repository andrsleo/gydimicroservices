package com.affiliate.rentals.gydi.social.infrastructure.out.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "shares", schema = "social")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class ShareEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "content_post_id", nullable = false) private Long contentPostId;
    @Column(name = "user_id") private Long userId;
    @Column(name = "platform", nullable = false, length = 20) private String platform;
    @Column(name = "created_at", nullable = false) private LocalDateTime createdAt;
    @PrePersist protected void onCreate() { if (createdAt == null) createdAt = LocalDateTime.now(); }
}
