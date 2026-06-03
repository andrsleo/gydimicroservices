package com.affiliate.rentals.gydi.content.infrastructure.out.persistence.adapter;

import com.affiliate.rentals.gydi.content.application.port.ContentPostRepositoryPort;
import com.affiliate.rentals.gydi.content.domain.model.ContentPost;
import com.affiliate.rentals.gydi.content.infrastructure.out.persistence.mapper.ContentPostEntityMapper;
import com.affiliate.rentals.gydi.content.infrastructure.out.persistence.repository.ContentPostJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ContentPostRepositoryAdapter implements ContentPostRepositoryPort {

    private final ContentPostJpaRepository jpaRepository;
    private final ContentPostEntityMapper mapper;

    @Override
    public ContentPost save(ContentPost post) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(post)));
    }

    @Override
    public Optional<ContentPost> findById(Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Page<ContentPost> findByCreatorId(Long creatorId, Pageable pageable) {
        return jpaRepository.findByCreatorId(creatorId, pageable).map(mapper::toDomain);
    }

    @Override
    public Page<ContentPost> findByPropertyId(Long propertyId, Pageable pageable) {
        return jpaRepository.findByPropertyId(propertyId, pageable).map(mapper::toDomain);
    }

    @Override
    public Page<ContentPost> findPublishedFeed(Pageable pageable) {
        return jpaRepository.findByStatus("PUBLISHED", pageable).map(mapper::toDomain);
    }

    @Override
    public void deleteById(Long id) {
        jpaRepository.deleteById(id);
    }

    @Override
    public List<ContentPost> findFeedByCursor(Long cursorId, int limit) {
        return jpaRepository.findFeedByCursor(cursorId, limit)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<ContentPost> findFollowingFeedByCursor(List<Long> creatorIds, Long cursorId, int limit) {
        if (creatorIds == null || creatorIds.isEmpty()) {
            return Collections.emptyList();
        }
        return jpaRepository.findFollowingFeedByCursor(creatorIds, cursorId, limit)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public void incrementViewCount(Long contentPostId) {
        jpaRepository.incrementViewCount(contentPostId);
    }

    // ─── Phase 5: Recommendation Engine ──────────────────────────────────────

    @Override
    public List<ContentPost> findSimilarContent(Long postId, int limit) {
        return jpaRepository.findSimilarContent(postId, limit)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<ContentPost> findRecommendedForUser(Long userId, int limit) {
        return jpaRepository.findRecommendedForUser(userId, limit)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<ContentPost> findCollaborativeRecommendations(Long postId, Long userId, int limit) {
        return jpaRepository.findCollaborativeRecommendations(postId, limit)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }
}
