package com.affiliate.rentals.gydi.calendar.application.usecase;

import com.affiliate.rentals.gydi.calendar.application.dto.SeasonPricingResponse;
import com.affiliate.rentals.gydi.calendar.application.dto.UpdateSeasonPricingRequest;
import com.affiliate.rentals.gydi.calendar.domain.model.PropertySeasonPricing;
import com.affiliate.rentals.gydi.calendar.domain.model.SeasonType;
import com.affiliate.rentals.gydi.calendar.domain.port.in.UpdateSeasonPricingUseCase;
import com.affiliate.rentals.gydi.calendar.domain.port.out.PropertySeasonPricingRepositoryPort;
import com.affiliate.rentals.gydi.bookings.domain.exception.UnauthorizedPropertyAccessException;
import com.affiliate.rentals.gydi.properties.domain.exception.PropertyNotFoundException;
import com.affiliate.rentals.gydi.properties.domain.model.Property;
import com.affiliate.rentals.gydi.properties.domain.model.PropertyId;
import com.affiliate.rentals.gydi.properties.domain.ports.out.PropertyRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.List;

@Service
@Transactional
public class UpdateSeasonPricingUseCaseImpl implements UpdateSeasonPricingUseCase {

    private final PropertyRepositoryPort propertyRepository;
    private final PropertySeasonPricingRepositoryPort seasonPricingRepository;

    public UpdateSeasonPricingUseCaseImpl(
            PropertyRepositoryPort propertyRepository,
            PropertySeasonPricingRepositoryPort seasonPricingRepository) {
        this.propertyRepository = propertyRepository;
        this.seasonPricingRepository = seasonPricingRepository;
    }

    @Override
    public SeasonPricingResponse execute(Long propertyId, UpdateSeasonPricingRequest request, Long requestingHostId) {
        Property property = propertyRepository.findById(PropertyId.of(propertyId))
                .orElseThrow(() -> new PropertyNotFoundException(String.valueOf(propertyId)));

        if (!property.getHostId().equals(requestingHostId)) {
            throw new UnauthorizedPropertyAccessException(
                    "Host " + requestingHostId + " does not own property " + propertyId);
        }

        seasonPricingRepository.deleteByPropertyId(propertyId);

        ZonedDateTime now = ZonedDateTime.now();
        List<PropertySeasonPricing> pricings = List.of(
                PropertySeasonPricing.reconstruct(null, propertyId, SeasonType.HIGH, request.highMultiplier(), now, now),
                PropertySeasonPricing.reconstruct(null, propertyId, SeasonType.MEDIUM, request.mediumMultiplier(), now, now),
                PropertySeasonPricing.reconstruct(null, propertyId, SeasonType.LOW, request.lowMultiplier(), now, now)
        );

        seasonPricingRepository.saveAll(pricings);

        return new SeasonPricingResponse(
                propertyId,
                request.highMultiplier().doubleValue(),
                request.mediumMultiplier().doubleValue(),
                request.lowMultiplier().doubleValue()
        );
    }
}
