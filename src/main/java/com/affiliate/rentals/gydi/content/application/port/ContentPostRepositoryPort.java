package com.affiliate.rentals.gydi.content.application.port;

import com.affiliate.rentals.gydi.content.domain.model.ContentPost;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

/**
 * Output port for ContentPost persistence operations.
 *
 * @author GYDI Development Team
 */
public interface ContentPostRepositoryPort {

    ContentPost save(ContentPost post);

    Optional<ContentPost> findById(Long id);

    Page<ContentPost> findByCreatorId(Long creatorId, Pageable pageable);

    Page<ContentPost> findByPropertyId(Long propertyId, Pageable pageable);

    Page<ContentPost> findPublishedFeed(Pageable pageable);

    void deleteById(Long id);

    /**
     * Returns a page of published posts ordered by feed_score DESC using the
     * feed_ranked materialized view. Cursor-based pagination.
     *
     * @param cursorId the ID of the last post seen (exclusive), or null for first page
     * @param limit    max number of posts to return
     */
    List<ContentPost> findFeedByCursor(Long cursorId, int limit);

    /**
     * Returns a page of published posts from a specific set of creator IDs,
     * ordered by publishedAt DESC. Cursor-based pagination.
     *
     * @param creatorIds list of creator user IDs to include
     * @param cursorId   the ID of the last post seen (exclusive), or null for first page
     * @param limit      max number of posts to return
     */
    List<ContentPost> findFollowingFeedByCursor(List<Long> creatorIds, Long cursorId, int limit);

    /**
     * Atomically increments the view_count for the given content post.
     *
     * @param contentPostId the ID of the content post
     */
    void incrementViewCount(Long contentPostId);

    // ─── Phase 5: Recommendation Engine ──────────────────────────────────────

    /**
     * Returns up to {@code limit} published posts similar to the given post.
     * Similarity is based on same property_id or same creator_id, ordered by
     * feed_score DESC.
     *
     * @param postId the reference post ID
     * @param limit  maximum number of results
     * @return list of similar content posts
     */
    List<ContentPost> findSimilarContent(Long postId, int limit);

    /**
     * Returns up to {@code limit} published posts recommended for the given user,
     * based on properties the user has already liked or saved. Excludes posts the
     * user already liked.
     *
     * @param userId the authenticated user ID
     * @param limit  maximum number of results
     * @return list of recommended content posts
     */
    List<ContentPost> findRecommendedForUser(Long userId, int limit);

    /**
     * Returns up to {@code limit} published posts via collaborative filtering:
     * "users who liked post X also liked post Y". Ordered by mutual like count
     * DESC, then feed_score DESC.
     *
     * @param postId the reference post ID
     * @param userId the authenticated user ID (excluded from own interactions)
     * @param limit  maximum number of results
     * @return list of collaboratively filtered content posts
     */
    List<ContentPost> findCollaborativeRecommendations(Long postId, Long userId, int limit);
}
