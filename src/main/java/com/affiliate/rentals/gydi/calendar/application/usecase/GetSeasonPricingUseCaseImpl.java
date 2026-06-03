package com.affiliate.rentals.gydi.calendar.application.usecase;

import com.affiliate.rentals.gydi.calendar.application.dto.SeasonPricingResponse;
import com.affiliate.rentals.gydi.calendar.domain.model.SeasonType;
import com.affiliate.rentals.gydi.calendar.domain.port.in.GetSeasonPricingUseCase;
import com.affiliate.rentals.gydi.calendar.domain.port.out.PropertySeasonPricingRepositoryPort;
import com.affiliate.rentals.gydi.properties.domain.exception.PropertyNotFoundException;
import com.affiliate.rentals.gydi.properties.domain.model.PropertyId;
import com.affiliate.rentals.gydi.properties.domain.ports.out.PropertyRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class GetSeasonPricingUseCaseImpl implements GetSeasonPricingUseCase {

    private final PropertyRepositoryPort propertyRepository;
    private final PropertySeasonPricingRepositoryPort seasonPricingRepository;

    public GetSeasonPricingUseCaseImpl(
            PropertyRepositoryPort propertyRepository,
            PropertySeasonPricingRepositoryPort seasonPricingRepository) {
        this.propertyRepository = propertyRepository;
        this.seasonPricingRepository = seasonPricingRepository;
    }

    @Override
    public SeasonPricingResponse execute(Long propertyId) {
        propertyRepository.findById(PropertyId.of(propertyId))
                .orElseThrow(() -> new PropertyNotFoundException(String.valueOf(propertyId)));

        Map<SeasonType, BigDecimal> multipliers = seasonPricingRepository.findMultipliersByPropertyId(propertyId);

        return new SeasonPricingResponse(
                propertyId,
                multipliers.getOrDefault(SeasonType.HIGH, new BigDecimal("1.50")).doubleValue(),
                multipliers.getOrDefault(SeasonType.MEDIUM, new BigDecimal("1.20")).doubleValue(),
                multipliers.getOrDefault(SeasonType.LOW, new BigDecimal("0.80")).doubleValue()
        );
    }
}
