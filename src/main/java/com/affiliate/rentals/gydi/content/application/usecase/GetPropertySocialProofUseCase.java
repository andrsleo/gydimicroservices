package com.affiliate.rentals.gydi.content.application.usecase;

import com.affiliate.rentals.gydi.content.application.dto.CreatorSummaryDto;
import com.affiliate.rentals.gydi.content.application.dto.PropertySocialProofDto;
import com.affiliate.rentals.gydi.content.application.port.ContentPostRepositoryPort;
import com.affiliate.rentals.gydi.referrals.domain.port.ContentAttributionRepositoryPort;
import com.affiliate.rentals.gydi.users.domain.model.UserProfile;
import com.affiliate.rentals.gydi.users.domain.ports.UserProfileRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GetPropertySocialProofUseCase {

    private final ContentPostRepositoryPort contentPostRepository;
    private final ContentAttributionRepositoryPort attributionRepository;
    private final UserProfileRepositoryPort userProfileRepository;

    @Transactional(readOnly = true)
    public PropertySocialProofDto execute(Long propertyId) {
        // Count content posts for property
        int contentCount = (int) contentPostRepository.findByPropertyId(propertyId,
                PageRequest.of(0, 1)).getTotalElements();

        // Sum total views from published posts
        long totalViews = contentPostRepository.findByPropertyId(propertyId,
                PageRequest.of(0, Integer.MAX_VALUE))
                .getContent()
                .stream()
                .mapToLong(p -> p.viewCount())
                .sum();

        // Total bookings attributed via content
        int totalBookings = attributionRepository.countTotalBookingsByPropertyId(propertyId);

        // Top creator summaries (up to 5)
        List<Long> topCreatorIds = attributionRepository.findTopCreatorIdsByPropertyId(propertyId);
        List<CreatorSummaryDto> topCreators = topCreatorIds.stream()
                .map(creatorId -> {
                    UserProfile profile = userProfileRepository.findById(creatorId).orElse(null);
                    String name = profile != null ? profile.firstName() : "Creator";
                    String avatar = profile != null ? profile.coverImageUrl() : null;
                    return new CreatorSummaryDto(creatorId, name, avatar);
                })
                .toList();

        return new PropertySocialProofDto(propertyId, contentCount, totalViews, totalBookings, topCreators);
    }
}
