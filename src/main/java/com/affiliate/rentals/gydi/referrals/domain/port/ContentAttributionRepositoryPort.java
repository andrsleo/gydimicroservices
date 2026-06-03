package com.affiliate.rentals.gydi.referrals.domain.port;

import com.affiliate.rentals.gydi.referrals.domain.model.ContentAttribution;

import java.util.List;
import java.util.Optional;

public interface ContentAttributionRepositoryPort {

    ContentAttribution save(ContentAttribution attribution);

    List<ContentAttribution> findByCreatorId(Long creatorId);

    Optional<ContentAttribution> findByBookingId(Long bookingId);

    List<ContentAttribution> findByContentPostId(Long contentPostId);

    /** Count bookings attributed to a specific content post */
    int countBookingsByContentPostId(Long contentPostId);

    /** Top creator IDs (by booking count) for content attributed to a property */
    List<Long> findTopCreatorIdsByPropertyId(Long propertyId);

    /** Total bookings attributed via creator content for a property */
    int countTotalBookingsByPropertyId(Long propertyId);
}
