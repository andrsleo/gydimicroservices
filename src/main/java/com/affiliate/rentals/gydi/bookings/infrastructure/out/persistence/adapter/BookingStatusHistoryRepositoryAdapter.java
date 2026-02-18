package com.affiliate.rentals.gydi.bookings.infrastructure.out.persistence.adapter;

import com.affiliate.rentals.gydi.bookings.domain.model.BookingStatusHistory;
import com.affiliate.rentals.gydi.bookings.domain.ports.BookingStatusHistoryRepositoryPort;
import com.affiliate.rentals.gydi.bookings.infrastructure.out.persistence.entity.BookingJpaEntity;
import com.affiliate.rentals.gydi.bookings.infrastructure.out.persistence.entity.BookingStatusHistoryJpaEntity;
import com.affiliate.rentals.gydi.bookings.infrastructure.out.persistence.mapper.BookingEntityMapper;
import com.affiliate.rentals.gydi.bookings.infrastructure.out.persistence.repository.BookingJpaRepository;
import com.affiliate.rentals.gydi.bookings.infrastructure.out.persistence.repository.BookingStatusHistoryJpaRepository;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Adapter implementing BookingStatusHistoryRepositoryPort using Spring Data JPA.
 */
@Component
public class BookingStatusHistoryRepositoryAdapter implements BookingStatusHistoryRepositoryPort {

    private final BookingStatusHistoryJpaRepository jpaRepository;
    private final BookingJpaRepository bookingJpaRepository;
    private final BookingEntityMapper entityMapper;

    public BookingStatusHistoryRepositoryAdapter(
        BookingStatusHistoryJpaRepository jpaRepository,
        BookingJpaRepository bookingJpaRepository,
        BookingEntityMapper entityMapper
    ) {
        this.jpaRepository = jpaRepository;
        this.bookingJpaRepository = bookingJpaRepository;
        this.entityMapper = entityMapper;
    }

    @Override
    public BookingStatusHistory save(BookingStatusHistory history) {
        BookingStatusHistoryJpaEntity entity = entityMapper.toEntity(history);

        // Set booking relationship
        if (history.getBookingId() != null) {
            BookingJpaEntity bookingEntity = bookingJpaRepository.findById(history.getBookingId())
                .orElseThrow(() -> new IllegalArgumentException("Booking not found: " + history.getBookingId()));
            entity.setBooking(bookingEntity);
        }

        BookingStatusHistoryJpaEntity saved = jpaRepository.save(entity);
        return entityMapper.toDomain(saved);
    }

    @Override
    public List<BookingStatusHistory> findByBookingId(Long bookingId) {
        List<BookingStatusHistoryJpaEntity> entities = jpaRepository.findByBookingIdOrderByCreatedAtAsc(bookingId);
        return entityMapper.toHistoryDomainList(entities);
    }

    @Override
    public List<BookingStatusHistory> findByChangedBy(Long userId) {
        List<BookingStatusHistoryJpaEntity> entities = jpaRepository.findByChangedBy(userId);
        return entityMapper.toHistoryDomainList(entities);
    }
}
