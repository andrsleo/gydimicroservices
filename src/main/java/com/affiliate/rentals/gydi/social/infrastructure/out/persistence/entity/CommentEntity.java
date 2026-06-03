package com.affiliate.rentals.gydi.social.infrastructure.out.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "comments", schema = "social")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class CommentEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "content_post_id", nullable = false) private Long contentPostId;
    @Column(name = "user_id", nullable = false) private Long userId;
    @Column(name = "parent_comment_id") private Long parentCommentId;
    @Column(name = "body", nullable = false, length = 300) private String body;
    @Column(name = "status", nullable = false, length = 20) private String status = "VISIBLE";
    @Column(name = "created_at", nullable = false) private LocalDateTime createdAt;
    @Column(name = "updated_at", nullable = false) private LocalDateTime updatedAt;
    @PrePersist protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (updatedAt == null) updatedAt = LocalDateTime.now();
    }
    @PreUpdate protected void onUpdate() { updatedAt = LocalDateTime.now(); }
}
