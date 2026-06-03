package com.affiliate.rentals.gydi.users.application.usecase;

import com.affiliate.rentals.gydi.content.infrastructure.out.persistence.repository.ContentPostJpaRepository;
import com.affiliate.rentals.gydi.referrals.infrastructure.out.persistence.ContentAttributionJpaEntity;
import com.affiliate.rentals.gydi.referrals.infrastructure.out.persistence.ContentAttributionJpaRepository;
import com.affiliate.rentals.gydi.users.application.dto.CreatorContentAnalyticsDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class GetCreatorTopContentUseCase {

    private final ContentPostJpaRepository contentPostJpaRepository;
    private final ContentAttributionJpaRepository contentAttributionJpaRepository;

    @Transactional(readOnly = true)
    public List<CreatorContentAnalyticsDto> execute(Long creatorId, int limit) {
        log.debug("Fetching top {} content for creatorId={}", limit, creatorId);

        // Fetch more posts to allow sorting by performance score
        var posts = contentPostJpaRepository.findByCreatorId(creatorId,
                PageRequest.of(0, Math.max(limit * 3, 30),
                        Sort.by(Sort.Direction.DESC, "engagementScore")));

        Map<Long, Long> bookingsByPost = contentAttributionJpaRepository.findByCreatorId(creatorId)
                .stream()
                .filter(a -> a.getBookingId() != null)
                .collect(Collectors.groupingBy(ContentAttributionJpaEntity::getContentPostId,
                        Collectors.counting()));

        return posts.stream()
                .map(post -> new CreatorContentAnalyticsDto(
                        post.getId(),
                        post.getCaption(),
                        post.getThumbnailUrl(),
                        post.getViewCount() != null ? post.getViewCount().longValue() : 0L,
                        post.getLikeCount() != null ? post.getLikeCount() : 0,
                        post.getSaveCount() != null ? post.getSaveCount() : 0,
                        bookingsByPost.getOrDefault(post.getId(), 0L).intValue(),
                        0.0,
                        post.getPublishedAt()
                ))
                .sorted(Comparator.comparingDouble(
                        (CreatorContentAnalyticsDto dto) ->
                                dto.views() * 0.3 + dto.likes() * 0.3
                                        + dto.bookingsGenerated() * 100.0 + dto.saves() * 0.2
                ).reversed())
                .limit(limit)
                .toList();
    }
}
