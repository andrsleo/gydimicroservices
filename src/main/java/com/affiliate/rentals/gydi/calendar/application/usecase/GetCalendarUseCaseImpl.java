package com.affiliate.rentals.gydi.calendar.application.usecase;

import com.affiliate.rentals.gydi.calendar.application.dto.CalendarDayDto;
import com.affiliate.rentals.gydi.calendar.application.dto.CalendarMonthResponse;
import com.affiliate.rentals.gydi.calendar.application.dto.MoneyDto;
import com.affiliate.rentals.gydi.calendar.application.dto.SeasonPricingDto;
import com.affiliate.rentals.gydi.calendar.domain.model.CalendarDay;
import com.affiliate.rentals.gydi.calendar.domain.model.CalendarDayStatus;
import com.affiliate.rentals.gydi.calendar.domain.model.SeasonDefinition;
import com.affiliate.rentals.gydi.calendar.domain.model.SeasonType;
import com.affiliate.rentals.gydi.calendar.domain.port.in.GetCalendarUseCase;
import com.affiliate.rentals.gydi.calendar.domain.port.out.CalendarDayRepositoryPort;
import com.affiliate.rentals.gydi.calendar.domain.port.out.PropertySeasonPricingRepositoryPort;
import com.affiliate.rentals.gydi.calendar.domain.port.out.SeasonDefinitionRepositoryPort;
import com.affiliate.rentals.gydi.calendar.domain.service.PriceCalculatorService;
import com.affiliate.rentals.gydi.calendar.domain.service.PriceCalculatorService.PricedNight;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class GetCalendarUseCaseImpl implements GetCalendarUseCase {

    private final PropertyRepositoryPort propertyRepository;
    private final CalendarDayRepositoryPort calendarDayRepository;
    private final SeasonDefinitionRepositoryPort seasonDefinitionRepository;
    private final PropertySeasonPricingRepositoryPort seasonPricingRepository;
    private final PriceCalculatorService priceCalculator = new PriceCalculatorService();
    private final SeasonDetectorService seasonDetector = new SeasonDetectorService();

    public GetCalendarUseCaseImpl(
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
    public CalendarMonthResponse execute(Long propertyId, int year, int month) {
        Property property = propertyRepository.findById(PropertyId.of(propertyId))
                .orElseThrow(() -> new PropertyNotFoundException(String.valueOf(propertyId)));

        LocalDate firstDay = LocalDate.of(year, month, 1);
        LocalDate lastDay = firstDay.withDayOfMonth(firstDay.lengthOfMonth());

        Map<LocalDate, CalendarDay> calendarDaysMap =
                calendarDayRepository.findByPropertyIdAndDateRangeAsMap(propertyId, firstDay, lastDay);

        String country = property.getLocation().country();
        List<SeasonDefinition> seasons = seasonDefinitionRepository.findByCountry(country);
        Map<SeasonType, BigDecimal> multipliers = seasonPricingRepository.findMultipliersByPropertyId(propertyId);

        Money basePrice = property.getPricePerNight();
        List<CalendarDayDto> days = new ArrayList<>();

        LocalDate current = firstDay;
        while (!current.isAfter(lastDay)) {
            CalendarDay calendarDay = calendarDaysMap.get(current);
            SeasonType activeSeason = seasonDetector.detectSeason(current, country, null, seasons);
            PricedNight pricedNight = priceCalculator.calculatePriceForDate(
                    current, basePrice, calendarDay, activeSeason, multipliers);

            String status = calendarDay != null ? calendarDay.getStatus().name() : "AVAILABLE";
            boolean isBlockedOrMaintenance = CalendarDayStatus.BLOCKED.name().equals(status)
                    || CalendarDayStatus.MAINTENANCE.name().equals(status);

            Double effectivePrice = isBlockedOrMaintenance ? null : pricedNight.price().amount().doubleValue();
            String effectivePriceCurrency = isBlockedOrMaintenance ? null
                    : pricedNight.price().currency().getCurrencyCode();
            String priceSource = isBlockedOrMaintenance ? null : pricedNight.source().name();
            Double customPrice = (calendarDay != null && calendarDay.hasCustomPrice())
                    ? calendarDay.getCustomPrice().amount().doubleValue()
                    : null;
            String blockSource = (calendarDay != null && calendarDay.getBlockSource() != null)
                    ? calendarDay.getBlockSource().name()
                    : null;
            Long bookingId = calendarDay != null ? calendarDay.getBookingId() : null;

            days.add(new CalendarDayDto(
                    current.toString(),
                    status,
                    effectivePrice,
                    effectivePriceCurrency,
                    priceSource,
                    customPrice,
                    blockSource,
                    bookingId));

            current = current.plusDays(1);
        }

        SeasonPricingDto seasonPricingDto = new SeasonPricingDto(
                multipliers.getOrDefault(SeasonType.HIGH, BigDecimal.ONE).doubleValue(),
                multipliers.getOrDefault(SeasonType.MEDIUM, BigDecimal.ONE).doubleValue(),
                multipliers.getOrDefault(SeasonType.LOW, BigDecimal.ONE).doubleValue());

        return new CalendarMonthResponse(
                propertyId,
                year,
                month,
                MoneyDto.of(basePrice),
                seasonPricingDto,
                days);
    }
}
