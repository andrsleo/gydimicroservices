package com.affiliate.rentals.gydi.calendar.infrastructure.out.persistence.adapter;

import com.affiliate.rentals.gydi.calendar.domain.model.BlockSource;
import com.affiliate.rentals.gydi.calendar.domain.model.CalendarDay;
import com.affiliate.rentals.gydi.calendar.domain.model.CalendarDayStatus;
import com.affiliate.rentals.gydi.calendar.domain.port.out.CalendarDayRepositoryPort;
import com.affiliate.rentals.gydi.calendar.infrastructure.out.persistence.entity.CalendarDayJpaEntity;
import com.affiliate.rentals.gydi.calendar.infrastructure.out.persistence.repository.CalendarDayJpaRepository;
import com.affiliate.rentals.gydi.properties.domain.model.Money;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class CalendarDayRepositoryAdapter implements CalendarDayRepositoryPort {

    private final CalendarDayJpaRepository jpaRepository;

    public CalendarDayRepositoryAdapter(CalendarDayJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public CalendarDay save(CalendarDay day) {
        CalendarDayJpaEntity entity = toEntity(day);
        CalendarDayJpaEntity saved = jpaRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public void saveAll(List<CalendarDay> days) {
        List<CalendarDayJpaEntity> entities = days.stream()
                .map(this::toEntity)
                .collect(Collectors.toList());
        jpaRepository.saveAll(entities);
    }

    @Override
    public Optional<CalendarDay> findByPropertyIdAndDate(Long propertyId, LocalDate date) {
        return jpaRepository.findByPropertyIdAndCalendarDate(propertyId, date)
                .map(this::toDomain);
    }

    @Override
    public List<CalendarDay> findByPropertyIdAndDateRange(Long propertyId, LocalDate from, LocalDate to) {
        return jpaRepository.findByPropertyIdAndCalendarDateBetweenOrderByCalendarDateAsc(propertyId, from, to)
                .stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Map<LocalDate, CalendarDay> findByPropertyIdAndDateRangeAsMap(Long propertyId, LocalDate from, LocalDate to) {
        return jpaRepository.findByPropertyIdAndCalendarDateBetweenOrderByCalendarDateAsc(propertyId, from, to)
                .stream()
                .map(this::toDomain)
                .collect(Collectors.toMap(CalendarDay::getCalendarDate, d -> d));
    }

    @Override
    @Transactional
    public void delete(Long propertyId, LocalDate date) {
        jpaRepository.deleteByPropertyIdAndCalendarDate(propertyId, date);
    }

    @Override
    @Transactional
    public void deleteByBookingId(Long bookingId) {
        jpaRepository.deleteByBookingId(bookingId);
    }

    @Override
    public boolean hasAnyNonAvailableDate(Long propertyId, LocalDate checkIn, LocalDate checkOut) {
        return jpaRepository.existsNonAvailableInRange(propertyId, checkIn, checkOut.minusDays(1));
    }

    // -------------------------------------------------------------------------
    // Mapping
    // -------------------------------------------------------------------------

    private CalendarDayJpaEntity toEntity(CalendarDay domain) {
        return CalendarDayJpaEntity.builder()
                .id(domain.getId())
                .propertyId(domain.getPropertyId())
                .calendarDate(domain.getCalendarDate())
                .status(domain.getStatus())
                .customPriceAmount(domain.getCustomPrice() != null ? domain.getCustomPrice().amount() : null)
                .customPriceCurrency(domain.getCustomPrice() != null ? domain.getCustomPrice().currency().getCurrencyCode() : null)
                .blockSource(domain.getBlockSource() != null ? domain.getBlockSource().name() : null)
                .bookingId(domain.getBookingId())
                .notes(domain.getNotes())
                .createdBy(domain.getCreatedBy())
                .createdAt(domain.getCreatedAt().toOffsetDateTime())
                .updatedAt(domain.getUpdatedAt().toOffsetDateTime())
                .build();
    }

    private CalendarDay toDomain(CalendarDayJpaEntity entity) {
        Money customPrice = entity.getCustomPriceAmount() != null
                ? Money.of(entity.getCustomPriceAmount(), entity.getCustomPriceCurrency())
                : null;

        BlockSource blockSource = entity.getBlockSource() != null
                ? BlockSource.valueOf(entity.getBlockSource())
                : null;

        return CalendarDay.reconstruct(
                entity.getId(),
                entity.getPropertyId(),
                entity.getCalendarDate(),
                entity.getStatus(),
                customPrice,
                blockSource,
                entity.getBookingId(),
                entity.getNotes(),
                entity.getCreatedBy(),
                entity.getCreatedAt().atZoneSameInstant(ZoneOffset.UTC),
                entity.getUpdatedAt().atZoneSameInstant(ZoneOffset.UTC)
        );
    }
}
