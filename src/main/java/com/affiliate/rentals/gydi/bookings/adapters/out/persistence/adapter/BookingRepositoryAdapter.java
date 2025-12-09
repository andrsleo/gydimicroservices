package com.affiliate.rentals.gydi.bookings.adapters.out.persistence.adapter;

import com.affiliate.rentals.gydi.bookings.adapters.out.persistence.entity.BookingEntity;
import com.affiliate.rentals.gydi.bookings.adapters.out.persistence.repository.BookingJpaRepository;
import com.affiliate.rentals.gydi.bookings.domain.model.Booking;
import com.affiliate.rentals.gydi.bookings.domain.model.BookingStatus;
import com.affiliate.rentals.gydi.bookings.domain.model.ClientInfo;
import com.affiliate.rentals.gydi.bookings.domain.model.DateRange;
import com.affiliate.rentals.gydi.bookings.domain.port.out.BookingRepositoryPort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Adapter implementing BookingRepositoryPort using JPA.
 * <p>
 * Translates between domain model (Booking) and persistence model (BookingEntity).
 * </p>
 */
@Component
public class BookingRepositoryAdapter implements BookingRepositoryPort {

    private final BookingJpaRepository jpaRepository;

    public BookingRepositoryAdapter(BookingJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Booking save(Booking booking) {
        BookingEntity entity = toEntity(booking);
        BookingEntity savedEntity = jpaRepository.save(entity);
        return toDomain(savedEntity);
    }

    @Override
    public Optional<Booking> findById(Long id) {
        return jpaRepository.findById(id)
            .map(this::toDomain);
    }

    @Override
    public List<Booking> findByReferralLinkId(Long referralLinkId) {
        return jpaRepository.findByReferralLinkId(referralLinkId)
            .stream()
            .map(this::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public boolean isPropertyAvailable(Long propertyId, DateRange dateRange) {
        // Property is available if there are NO overlapping bookings
        boolean hasOverlap = jpaRepository.existsOverlappingBooking(
            propertyId,
            dateRange.getStartDate(),
            dateRange.getEndDate()
        );
        return !hasOverlap;
    }

    @Override
    public List<Booking> findActiveBookingsByProperty(Long propertyId) {
        return jpaRepository.findActiveBookingsByProperty(propertyId)
            .stream()
            .map(this::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<Booking> findByStatus(String status) {
        BookingEntity.BookingStatusEntity statusEntity =
            BookingEntity.BookingStatusEntity.valueOf(status);

        return jpaRepository.findByStatus(statusEntity)
            .stream()
            .map(this::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public void deleteById(Long id) {
        jpaRepository.deleteById(id);
    }

    // ========== MAPPING: Entity ↔ Domain ==========

    /**
     * Converts domain model to JPA entity.
     */
    private BookingEntity toEntity(Booking booking) {
        BookingEntity entity = BookingEntity.create()
            .referralLinkId(booking.getReferralLinkId())
            .propertyId(booking.getPropertyId())
            .startDate(booking.getDateRange().getStartDate())
            .endDate(booking.getDateRange().getEndDate())
            .clientEmail(booking.getClientInfo().getEmail())
            .clientPhone(booking.getClientInfo().getPhone())
            .clientFirstName(booking.getClientInfo().getFirstName())
            .clientLastName(booking.getClientInfo().getLastName())
            .status(toEntityStatus(booking.getStatus()));

        // Set ID if exists (for updates)
        if (booking.getId() != null) {
            entity.setId(booking.getId());
        }

        return entity;
    }

    /**
     * Converts JPA entity to domain model.
     */
    private Booking toDomain(BookingEntity entity) {
        DateRange dateRange = new DateRange(entity.getStartDate(), entity.getEndDate());

        ClientInfo clientInfo = new ClientInfo(
            entity.getClientEmail(),
            entity.getClientPhone(),
            entity.getClientFirstName(),
            entity.getClientLastName()
        );

        return Booking.reconstitute(
            entity.getId(),
            entity.getReferralLinkId(),
            entity.getPropertyId(),
            dateRange,
            clientInfo,
            toDomainStatus(entity.getStatus()),
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }

    /**
     * Converts domain BookingStatus to entity BookingStatusEntity.
     */
    private BookingEntity.BookingStatusEntity toEntityStatus(BookingStatus domainStatus) {
        return BookingEntity.BookingStatusEntity.valueOf(domainStatus.name());
    }

    /**
     * Converts entity BookingStatusEntity to domain BookingStatus.
     */
    private BookingStatus toDomainStatus(BookingEntity.BookingStatusEntity entityStatus) {
        return BookingStatus.valueOf(entityStatus.name());
    }
}
