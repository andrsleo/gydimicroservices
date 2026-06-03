package com.affiliate.rentals.gydi.content.application.usecase;

import com.affiliate.rentals.gydi.content.application.dto.AttributionFunnelDto;
import com.affiliate.rentals.gydi.content.application.port.AdminAnalyticsRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Use case: retrieve the content-to-booking attribution funnel for the Admin dashboard.
 *
 * <p>Computes the following metrics:
 * <ul>
 *   <li><b>totalBookingsFromContent</b> — distinct bookings in {@code referrals.content_attributions}.</li>
 *   <li><b>conversionRate</b> — bookings from content ÷ total content views (0.0 if no views).</li>
 *   <li><b>avgAttributedRevenue</b> — average {@code total_price} of attributed bookings.</li>
 *   <li><b>totalContentViews</b> — sum of {@code view_count} for all published posts.</li>
 *   <li><b>totalContentWithProperty</b> — published posts linked to a property.</li>
 * </ul>
 * </p>
 *
 * @author GYDI Development Team
 */
@Service
@RequiredArgsConstructor
public class GetAttributionFunnelUseCase {

    private final AdminAnalyticsRepositoryPort analyticsRepository;

    @Transactional(readOnly = true)
    public AttributionFunnelDto execute() {
        Integer totalBookingsFromContent = analyticsRepository.getTotalBookingsFromContent();
        Double  avgAttributedRevenue     = analyticsRepository.getAvgAttributedRevenue();
        Long    totalContentViews        = analyticsRepository.getTotalViewsFromStats();
        Long    totalContentWithProperty = analyticsRepository.getTotalContentWithProperty();

        double conversionRate = totalContentViews > 0
                ? (double) totalBookingsFromContent / totalContentViews
                : 0.0;

        return new AttributionFunnelDto(
                totalBookingsFromContent,
                conversionRate,
                avgAttributedRevenue,
                totalContentViews,
                totalContentWithProperty
        );
    }
}
