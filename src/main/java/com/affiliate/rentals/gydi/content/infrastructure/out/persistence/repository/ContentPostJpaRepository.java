package com.affiliate.rentals.gydi.content.infrastructure.out.persistence.repository;

import com.affiliate.rentals.gydi.content.infrastructure.out.persistence.entity.ContentPostEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ContentPostJpaRepository extends JpaRepository<ContentPostEntity, Long> {

    Page<ContentPostEntity> findByCreatorId(Long creatorId, Pageable pageable);

    Page<ContentPostEntity> findByPropertyId(Long propertyId, Pageable pageable);

    Page<ContentPostEntity> findByStatus(String status, Pageable pageable);

    @Query("SELECT COUNT(DISTINCT r.reporterId) FROM ContentReportEntity r WHERE r.contentPostId = :contentPostId")
    long countDistinctReportersByContentPostId(@Param("contentPostId") Long contentPostId);

    @Query(value = """
            SELECT cp.* FROM content.content_posts cp
            INNER JOIN content.feed_ranked fr ON fr.id = cp.id
            WHERE (:cursorId IS NULL
                OR (fr.feed_score, fr.id) < (
                    (SELECT f2.feed_score FROM content.feed_ranked f2 WHERE f2.id = :cursorId),
                    CAST(:cursorId AS BIGINT)
                ))
            ORDER BY fr.feed_score DESC, cp.id DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<ContentPostEntity> findFeedByCursor(@Param("cursorId") Long cursorId, @Param("limit") int limit);

    @Query(value = """
            SELECT cp.* FROM content.content_posts cp
            WHERE cp.creator_id IN (:creatorIds)
            AND cp.status = 'PUBLISHED'
            AND (:cursorId IS NULL OR cp.id < :cursorId)
            ORDER BY cp.published_at DESC, cp.id DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<ContentPostEntity> findFollowingFeedByCursor(
            @Param("creatorIds") List<Long> creatorIds,
            @Param("cursorId") Long cursorId,
            @Param("limit") int limit);

    @Modifying
    @Query("UPDATE ContentPostEntity cp SET cp.viewCount = cp.viewCount + 1 WHERE cp.id = :id")
    void incrementViewCount(@Param("id") Long id);

    // ─── Phase 5: Recommendation Engine ──────────────────────────────────────

    @Query(value = """
            SELECT DISTINCT cp.* FROM content.content_posts cp
            WHERE cp.id != :postId
            AND cp.status = 'PUBLISHED'
            AND (
                cp.property_id = (SELECT property_id FROM content.content_posts WHERE id = :postId)
                OR cp.creator_id = (SELECT creator_id FROM content.content_posts WHERE id = :postId)
            )
            ORDER BY cp.feed_score DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<ContentPostEntity> findSimilarContent(
            @Param("postId") Long postId,
            @Param("limit") int limit);

    @Query(value = """
            SELECT DISTINCT cp.* FROM content.content_posts cp
            WHERE cp.status = 'PUBLISHED'
            AND cp.property_id IN (
                SELECT DISTINCT cp2.property_id FROM content.content_posts cp2
                JOIN social.likes l ON l.content_post_id = cp2.id
                WHERE l.user_id = :userId
                UNION
                SELECT DISTINCT cp3.property_id FROM content.content_posts cp3
                JOIN social.saves s ON s.content_post_id = cp3.id
                WHERE s.user_id = :userId
            )
            AND cp.id NOT IN (
                SELECT content_post_id FROM social.likes WHERE user_id = :userId
            )
            ORDER BY cp.feed_score DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<ContentPostEntity> findRecommendedForUser(
            @Param("userId") Long userId,
            @Param("limit") int limit);

    @Query(value = """
            SELECT cp.*, COUNT(*) AS mutual_likes FROM content.content_posts cp
            JOIN social.likes l2 ON l2.content_post_id = cp.id
            JOIN social.likes l1 ON l1.user_id = l2.user_id
            WHERE l1.content_post_id = :postId
            AND l2.content_post_id != :postId
            AND cp.status = 'PUBLISHED'
            GROUP BY cp.id, cp.creator_id, cp.property_id, cp.caption, cp.type,
                     cp.status, cp.thumbnail_url, cp.view_count, cp.like_count,
                     cp.save_count, cp.comment_count, cp.share_count,
                     cp.engagement_score, cp.feed_score, cp.published_at,
                     cp.created_at, cp.updated_at
            ORDER BY mutual_likes DESC, cp.feed_score DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<ContentPostEntity> findCollaborativeRecommendations(
            @Param("postId") Long postId,
            @Param("limit") int limit);
}
