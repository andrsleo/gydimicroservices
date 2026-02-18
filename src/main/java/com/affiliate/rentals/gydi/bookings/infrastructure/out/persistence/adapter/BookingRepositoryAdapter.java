package com.affiliate.rentals.gydi.bookings.infrastructure.out.persistence.adapter;

import com.affiliate.rentals.gydi.bookings.domain.model.Booking;
import com.affiliate.rentals.gydi.bookings.domain.model.BookingStatus;
import com.affiliate.rentals.gydi.bookings.domain.ports.BookingRepositoryPort;
import com.affiliate.rentals.gydi.bookings.infrastructure.out.persistence.entity.BookingJpaEntity;
import com.affiliate.rentals.gydi.bookings.infrastructure.out.persistence.mapper.BookingEntityMapper;
import com.affiliate.rentals.gydi.bookings.infrastructure.out.persistence.repository.BookingJpaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Adapter implementing BookingRepositoryPort using Spring Data JPA.
 * <p>
 * Bridges domain layer (port) with infrastructure layer (JPA repository).
 * </p>
 */
@Component
public class BookingRepositoryAdapter implements BookingRepositoryPort {
    private static final Logger log = LoggerFactory.getLogger(BookingRepositoryAdapter.class);

    private final BookingJpaRepository jpaRepository;
    private final BookingEntityMapper entityMapper;

    public BookingRepositoryAdapter(BookingJpaRepository jpaRepository, BookingEntityMapper entityMapper) {
        this.jpaRepository = jpaRepository;
        this.entityMapper = entityMapper;
    }

    @Override
    public Booking save(Booking booking) {
        BookingJpaEntity entity = entityMapper.toEntity(booking);
        BookingJpaEntity saved = jpaRepository.save(entity);

        // Update domain model ID if it was newly created
        if (booking.getId() == null && saved.getId() != null) {
            booking.setId(saved.getId());
        }

        return entityMapper.toDomain(saved);
    }

    @Override
    public Optional<Booking> findById(Long bookingId) {
        return jpaRepository.findById(bookingId)
                .map(entityMapper::toDomain);
    }

    @Override
    public List<Booking> findByPropertyId(Long propertyId) {
        List<BookingJpaEntity> entities = jpaRepository.findByPropertyId(propertyId);
        return entityMapper.toDomainList(entities);
    }

    @Override
    public List<Booking> findByReferralLinkId(Long referralLinkId) {
        List<BookingJpaEntity> entities = jpaRepository.findByReferralLinkId(referralLinkId);
        return entityMapper.toDomainList(entities);
    }

    @Override
    public List<Booking> findByStatus(BookingStatus status) {
        List<BookingJpaEntity> entities = jpaRepository.findByStatus(status);
        return entityMapper.toDomainList(entities);
    }

    @Override
    public List<Booking> findReadyToStart(LocalDate currentDate) {
        List<BookingJpaEntity> entities = jpaRepository.findReadyToStart(currentDate);
        return entityMapper.toDomainList(entities);
    }

    @Override
    public List<Booking> findReadyToFinish(LocalDate currentDate) {
        List<BookingJpaEntity> entities = jpaRepository.findReadyToFinish(currentDate);
        return entityMapper.toDomainList(entities);
    }

    @Override
    public List<Booking> findAll() {
        List<BookingJpaEntity> entities = jpaRepository.findAll();
        return entityMapper.toDomainList(entities);
    }

    @Override
    public void deleteById(Long bookingId) {
        jpaRepository.deleteById(bookingId);
    }

    @Override
    public boolean existsById(Long bookingId) {
        return jpaRepository.existsById(bookingId);
    }

    @Override
    public boolean existsOverlappingBooking(Long propertyId, LocalDate checkInDate, LocalDate checkOutDate) {
        return jpaRepository.existsOverlappingBooking(propertyId, checkInDate, checkOutDate);
    }

    @Override
    public Optional<Long> findHostIdByPropertyId(Long propertyId) {
        return Optional.ofNullable(jpaRepository.findHostIdByPropertyId(propertyId));
    }

    @Override
    public Optional<Long> findAffiliateIdByReferralLinkId(Long referralLinkId) {
        return Optional.ofNullable(jpaRepository.findAffiliateIdByReferralLinkId(referralLinkId));
    }
}
