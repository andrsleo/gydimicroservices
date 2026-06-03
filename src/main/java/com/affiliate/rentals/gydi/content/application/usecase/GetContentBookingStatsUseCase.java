package com.affiliate.rentals.gydi.content.application.usecase;

import com.affiliate.rentals.gydi.content.application.dto.ContentBookingStatsDto;
import com.affiliate.rentals.gydi.content.application.port.ContentPostRepositoryPort;
import com.affiliate.rentals.gydi.content.domain.model.ContentPost;
import com.affiliate.rentals.gydi.referrals.domain.port.ContentAttributionRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetContentBookingStatsUseCase {

    private final ContentPostRepositoryPort contentPostRepository;
    private final ContentAttributionRepositoryPort attributionRepository;

    @Transactional(readOnly = true)
    public ContentBookingStatsDto execute(Long contentPostId) {
        ContentPost post = contentPostRepository.findById(contentPostId)
                .orElseThrow(() -> new IllegalArgumentException("Content post not found: " + contentPostId));

        int bookingCount = attributionRepository.countBookingsByContentPostId(contentPostId);
        long totalViews = post.viewCount();

        // conversionRate: bookings per view (0.0 if no views)
        double conversionRate = totalViews > 0 ? (double) bookingCount / totalViews : 0.0;

        return new ContentBookingStatsDto(contentPostId, bookingCount, null, conversionRate);
    }
}
