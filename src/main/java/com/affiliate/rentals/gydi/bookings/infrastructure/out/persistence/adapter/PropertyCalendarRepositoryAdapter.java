package com.affiliate.rentals.gydi.bookings.infrastructure.out.persistence.adapter;

import com.affiliate.rentals.gydi.bookings.domain.model.PropertyCalendar;
import com.affiliate.rentals.gydi.bookings.domain.model.PropertyCalendar.BlockReason;
import com.affiliate.rentals.gydi.bookings.domain.ports.PropertyCalendarRepositoryPort;
import com.affiliate.rentals.gydi.bookings.infrastructure.out.persistence.entity.PropertyCalendarJpaEntity;
import com.affiliate.rentals.gydi.bookings.infrastructure.out.persistence.repository.PropertyCalendarJpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

/**
 * Adapter implementing PropertyCalendarRepositoryPort using Spring Data JPA.
 * <p>
 * Bridges domain port with JPA repository.
 * Handles mapping between domain model and JPA entity.
 * </p>
 */
@Component
public class PropertyCalendarRepositoryAdapter implements PropertyCalendarRepositoryPort {

    private final PropertyCalendarJpaRepository jpaRepository;

    public PropertyCalendarRepositoryAdapter(PropertyCalendarJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    @Transactional
    public void save(PropertyCalendar entry) {
        jpaRepository.save(toEntity(entry));
    }

    @Override
    @Transactional
    public void saveAll(List<PropertyCalendar> entries) {
        List<PropertyCalendarJpaEntity> entities = entries.stream()
                .map(this::toEntity)
                .toList();
        jpaRepository.saveAll(entities);
    }

    @Override
    public List<PropertyCalendar> findByPropertyIdBetween(Long propertyId, LocalDate from, LocalDate to) {
        return jpaRepository
                .findByPropertyIdAndBlockedDateBetweenOrderByBlockedDateAsc(propertyId, from, to)
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public boolean isDateBlocked(Long propertyId, LocalDate date) {
        return jpaRepository.existsByPropertyIdAndBlockedDate(propertyId, date);
    }

    @Override
    public boolean hasAnyBlockedDate(Long propertyId, LocalDate checkIn, LocalDate checkOut) {
        return jpaRepository.existsBlockedDateInRange(propertyId, checkIn, checkOut);
    }

    @Override
    @Transactional
    public void deleteByPropertyIdAndDate(Long propertyId, LocalDate date) {
        jpaRepository.deleteByPropertyIdAndBlockedDate(propertyId, date);
    }

    @Override
    @Transactional
    public void deleteByBookingId(Long bookingId) {
        jpaRepository.deleteByBookingId(bookingId);
    }

    // ========== MAPPING ==========

    private PropertyCalendarJpaEntity toEntity(PropertyCalendar domain) {
        PropertyCalendarJpaEntity entity = new PropertyCalendarJpaEntity();
        entity.setId(domain.getId());
        entity.setPropertyId(domain.getPropertyId());
        entity.setBlockedDate(domain.getBlockedDate());
        entity.setBlockReason(domain.getBlockReason());
        entity.setBookingId(domain.getBookingId());
        entity.setCreatedByHostId(domain.getCreatedByHostId());
        entity.setCreatedAt(domain.getCreatedAt() != null
                ? domain.getCreatedAt().toOffsetDateTime()
                : OffsetDateTime.now(ZoneOffset.UTC));
        return entity;
    }

    private PropertyCalendar toDomain(PropertyCalendarJpaEntity entity) {
        return new PropertyCalendar(
                entity.getId(),
                entity.getPropertyId(),
                entity.getBlockedDate(),
                entity.getBlockReason(),
                entity.getBookingId(),
                entity.getCreatedByHostId(),
                entity.getCreatedAt() != null
                        ? entity.getCreatedAt().atZoneSameInstant(ZoneOffset.UTC)
                        : null
        );
    }
}
