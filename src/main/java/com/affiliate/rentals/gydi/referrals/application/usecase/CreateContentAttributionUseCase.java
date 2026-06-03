package com.affiliate.rentals.gydi.referrals.application.usecase;

import com.affiliate.rentals.gydi.referrals.application.dto.ContentAttributionDto;
import com.affiliate.rentals.gydi.referrals.application.dto.CreateContentAttributionRequest;
import com.affiliate.rentals.gydi.referrals.domain.model.AttributionType;
import com.affiliate.rentals.gydi.referrals.domain.model.ContentAttribution;
import com.affiliate.rentals.gydi.referrals.domain.port.ContentAttributionRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CreateContentAttributionUseCase {

    private final ContentAttributionRepositoryPort attributionRepository;

    @Transactional
    public ContentAttributionDto execute(CreateContentAttributionRequest request) {
        log.info("Creating content attribution: contentPostId={}, creatorId={}, type={}",
                request.contentPostId(), request.creatorId(), request.attributionType());

        AttributionType type;
        try {
            type = AttributionType.valueOf(request.attributionType());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid attributionType: " + request.attributionType()
                    + ". Valid values: DIRECT_VIEW, SHARED_LINK, CREATOR_LINK");
        }

        ContentAttribution attribution = ContentAttribution.create(
                request.bookingId(),
                request.contentPostId(),
                request.creatorId(),
                request.referralLinkId(),
                type
        );

        ContentAttribution saved = attributionRepository.save(attribution);

        log.info("Content attribution created with id={}", saved.getId());

        return toDto(saved);
    }

    private ContentAttributionDto toDto(ContentAttribution attribution) {
        return new ContentAttributionDto(
                attribution.getId(),
                attribution.getBookingId(),
                attribution.getContentPostId(),
                attribution.getCreatorId(),
                attribution.getReferralLinkId(),
                attribution.getAttributionType().name(),
                attribution.getCreatedAt()
        );
    }
}
