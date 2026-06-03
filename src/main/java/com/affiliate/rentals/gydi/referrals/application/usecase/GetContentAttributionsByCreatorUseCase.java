package com.affiliate.rentals.gydi.referrals.application.usecase;

import com.affiliate.rentals.gydi.referrals.application.dto.ContentAttributionDto;
import com.affiliate.rentals.gydi.referrals.domain.model.ContentAttribution;
import com.affiliate.rentals.gydi.referrals.domain.port.ContentAttributionRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class GetContentAttributionsByCreatorUseCase {

    private final ContentAttributionRepositoryPort attributionRepository;

    @Transactional(readOnly = true)
    public List<ContentAttributionDto> execute(Long creatorId) {
        log.debug("Fetching content attributions for creatorId={}", creatorId);

        return attributionRepository.findByCreatorId(creatorId)
                .stream()
                .map(this::toDto)
                .toList();
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
