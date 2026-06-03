package com.affiliate.rentals.gydi.calendar.domain.port.out;

import com.affiliate.rentals.gydi.calendar.domain.model.PropertySeasonPricing;
import com.affiliate.rentals.gydi.calendar.domain.model.SeasonType;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface PropertySeasonPricingRepositoryPort {
    List<PropertySeasonPricing> findByPropertyId(Long propertyId);
    Map<SeasonType, BigDecimal> findMultipliersByPropertyId(Long propertyId);
    PropertySeasonPricing save(PropertySeasonPricing pricing);
    void saveAll(List<PropertySeasonPricing> pricings);
    void deleteByPropertyId(Long propertyId);
}
