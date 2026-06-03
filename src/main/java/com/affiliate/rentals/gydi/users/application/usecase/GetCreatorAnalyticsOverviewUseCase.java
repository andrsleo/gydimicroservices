package com.affiliate.rentals.gydi.users.application.usecase;

import com.affiliate.rentals.gydi.referrals.infrastructure.out.persistence.ContentAttributionJpaRepository;
import com.affiliate.rentals.gydi.users.application.dto.CreatorAnalyticsOverviewDto;
import com.affiliate.rentals.gydi.users.domain.model.CreatorProfile;
import com.affiliate.rentals.gydi.users.domain.ports.CreatorProfileRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class GetCreatorAnalyticsOverviewUseCase {

    private final CreatorProfileRepositoryPort creatorProfileRepository;
    private final ContentAttributionJpaRepository contentAttributionJpaRepository;

    @Transactional(readOnly = true)
    public CreatorAnalyticsOverviewDto execute(Long creatorId) {
        log.debug("Fetching analytics overview for creatorId={}", creatorId);

        CreatorProfile profile = creatorProfileRepository.findByUserId(creatorId)
                .orElse(null);

        if (profile == null) {
            return new CreatorAnalyticsOverviewDto(0L, 0, 0, 0, 0.0, 0, 0, 0.0, null);
        }

        long totalBookings = contentAttributionJpaRepository.findByCreatorId(creatorId).size();

        return new CreatorAnalyticsOverviewDto(
                profile.totalViews(),
                0,
                0,
                (int) totalBookings,
                0.0,
                profile.followerCount(),
                profile.contentCount(),
                profile.avgEngagementRate(),
                profile.tier()
        );
    }
}
