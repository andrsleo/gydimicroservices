package com.affiliate.rentals.gydi.calendar.application.usecase;

import com.affiliate.rentals.gydi.calendar.application.dto.MoneyDto;
import com.affiliate.rentals.gydi.calendar.application.dto.PriceBreakdownResponse;
import com.affiliate.rentals.gydi.calendar.domain.model.CalendarDay;
import com.affiliate.rentals.gydi.calendar.domain.model.PriceBreakdown;
import com.affiliate.rentals.gydi.calendar.domain.model.SeasonDefinition;
import com.affiliate.rentals.gydi.calendar.domain.model.SeasonType;
import com.affiliate.rentals.gydi.calendar.domain.port.in.CalculatePriceUseCase;
import com.affiliate.rentals.gydi.calendar.domain.port.out.CalendarDayRepositoryPort;
import com.affiliate.rentals.gydi.calendar.domain.port.out.PropertySeasonPricingRepositoryPort;
import com.affiliate.rentals.gydi.calendar.domain.port.out.SeasonDefinitionRepositoryPort;
import com.affiliate.rentals.gydi.calendar.domain.service.PriceCalculatorService;
import com.affiliate.rentals.gydi.calendar.domain.service.SeasonDetectorService;
import com.affiliate.rentals.gydi.properties.domain.exception.PropertyNotFoundException;
import com.affiliate.rentals.gydi.properties.domain.model.Money;
import com.affiliate.rentals.gydi.properties.domain.model.Property;
import com.affiliate.rentals.gydi.properties.domain.model.PropertyId;
import com.affiliate.rentals.gydi.properties.domain.ports.out.PropertyRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class CalculatePriceUseCaseImpl implements CalculatePriceUseCase {

    private final PropertyRepositoryPort propertyRepository;
    private final CalendarDayRepositoryPort calendarDayRepository;
    private final SeasonDefinitionRepositoryPort seasonDefinitionRepository;
    private final PropertySeasonPricingRepositoryPort seasonPricingRepository;
    private final PriceCalculatorService priceCalculator = new PriceCalculatorService();
    private final SeasonDetectorService seasonDetector = new SeasonDetectorService();

    public CalculatePriceUseCaseImpl(
            PropertyRepositoryPort propertyRepository,
            CalendarDayRepositoryPort calendarDayRepository,
            SeasonDefinitionRepositoryPort seasonDefinitionRepository,
            PropertySeasonPricingRepositoryPort seasonPricingRepository) {
        this.propertyRepository = propertyRepository;
        this.calendarDayRepository = calendarDayRepository;
        this.seasonDefinitionRepository = seasonDefinitionRepository;
        this.seasonPricingRepository = seasonPricingRepository;
    }

    @Override
    public PriceBreakdownResponse execute(Long propertyId, LocalDate checkIn, LocalDate checkOut) {
        if (!checkIn.isBefore(checkOut)) {
            throw new IllegalArgumentException("checkIn must be before checkOut");
        }
        if (checkIn.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("checkIn cannot be in the past");
        }

        Property property = propertyRepository.findById(PropertyId.of(propertyId))
                .orElseThrow(() -> new PropertyNotFoundException(String.valueOf(propertyId)));

        Money basePrice = property.getPricePerNight();
        String country = property.getLocation().country();

        Map<LocalDate, CalendarDay> calendarDaysMap =
                calendarDayRepository.findByPropertyIdAndDateRangeAsMap(propertyId, checkIn, checkOut);

        List<SeasonDefinition> seasons = seasonDefinitionRepository.findByCountry(country);
        Map<SeasonType, BigDecimal> multipliers = seasonPricingRepository.findMultipliersByPropertyId(propertyId);

        PriceBreakdown breakdown = priceCalculator.calculateTotalForRange(
                checkIn,
                checkOut,
                basePrice,
                country,
                null,
                calendarDaysMap,
                seasons,
                multipliers,
                seasonDetector);

        List<PriceBreakdownResponse.DayPriceDto> nights = breakdown.getDays().stream()
                .map(d -> new PriceBreakdownResponse.DayPriceDto(
                        d.date().toString(),
                        MoneyDto.of(d.price()),
                        d.priceSource().name(),
                        d.status().name()))
                .toList();

        return new PriceBreakdownResponse(
                breakdown.getCheckIn().toString(),
                breakdown.getCheckOut().toString(),
                nights,
                MoneyDto.of(breakdown.getTotalPrice()));
    }
}
