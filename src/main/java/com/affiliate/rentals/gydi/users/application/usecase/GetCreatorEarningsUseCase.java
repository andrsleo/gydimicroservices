package com.affiliate.rentals.gydi.users.application.usecase;

import com.affiliate.rentals.gydi.content.infrastructure.out.persistence.repository.ContentPostJpaRepository;
import com.affiliate.rentals.gydi.referrals.infrastructure.out.persistence.ContentAttributionJpaRepository;
import com.affiliate.rentals.gydi.users.application.dto.CreatorEarningsDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class GetCreatorEarningsUseCase {

    private final ContentAttributionJpaRepository contentAttributionJpaRepository;
    private final ContentPostJpaRepository contentPostJpaRepository;

    @Transactional(readOnly = true)
    public List<CreatorEarningsDto> execute(Long creatorId) {
        log.debug("Fetching earnings breakdown for creatorId={}", creatorId);

        var attributions = contentAttributionJpaRepository.findByCreatorId(creatorId);

        // Build caption lookup map to avoid N+1
        var postIds = attributions.stream()
                .map(a -> a.getContentPostId())
                .collect(java.util.stream.Collectors.toSet());

        Map<Long, String> captionByPostId = contentPostJpaRepository.findAllById(postIds)
                .stream()
                .collect(Collectors.toMap(
                        post -> post.getId(),
                        post -> post.getCaption() != null ? post.getCaption() : ""
                ));

        return attributions.stream()
                .map(attr -> new CreatorEarningsDto(
                        attr.getContentPostId(),
                        captionByPostId.getOrDefault(attr.getContentPostId(), ""),
                        attr.getBookingId(),
                        attr.getAttributionType(),
                        0.0, // commission amount — to be enriched when commissions context is integrated
                        attr.getCreatedAt()
                ))
                .toList();
    }
}
