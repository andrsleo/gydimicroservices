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

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class GetCreatorContentAnalyticsUseCase {

    private final ContentPostJpaRepository contentPostJpaRepository;
    private final ContentAttributionJpaRepository contentAttributionJpaRepository;

    @Transactional(readOnly = true)
    public List<CreatorContentAnalyticsDto> execute(Long creatorId, int page, int size) {
        log.debug("Fetching content analytics for creatorId={}", creatorId);

        var posts = contentPostJpaRepository.findByCreatorId(creatorId,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "publishedAt")));

        // Group attributions by contentPostId for booking count
        Map<Long, Long> bookingsByPost = contentAttributionJpaRepository.findByCreatorId(creatorId)
                .stream()
                .filter(a -> a.getBookingId() != null)
                .collect(Collectors.groupingBy(ContentAttributionJpaEntity::getContentPostId,
                        Collectors.counting()));

        return posts.stream().map(post -> new CreatorContentAnalyticsDto(
                post.getId(),
                post.getCaption(),
                post.getThumbnailUrl(),
                post.getViewCount() != null ? post.getViewCount().longValue() : 0L,
                post.getLikeCount() != null ? post.getLikeCount() : 0,
                post.getSaveCount() != null ? post.getSaveCount() : 0,
                bookingsByPost.getOrDefault(post.getId(), 0L).intValue(),
                0.0, // earnings calculated separately via commissions context
                post.getPublishedAt()
        )).toList();
    }
}
