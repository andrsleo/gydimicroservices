package com.affiliate.rentals.gydi.social.infrastructure.out.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "saves", schema = "social",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "content_post_id"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class SaveEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "user_id", nullable = false) private Long userId;
    @Column(name = "content_post_id", nullable = false) private Long contentPostId;
    @Column(name = "created_at", nullable = false) private LocalDateTime createdAt;
    @PrePersist protected void onCreate() { if (createdAt == null) createdAt = LocalDateTime.now(); }
}
