package com.affiliate.rentals.gydi.payment.adapters.out.persistence.adapter;

import com.affiliate.rentals.gydi.payment.adapters.out.persistence.entity.PaymentBookingEntity;
import com.affiliate.rentals.gydi.payment.adapters.out.persistence.repository.PaymentJpaRepository;
import com.affiliate.rentals.gydi.payment.domain.model.Money;
import com.affiliate.rentals.gydi.payment.domain.model.PaymentBooking;
import com.affiliate.rentals.gydi.payment.domain.model.PaymentStatus;
import com.affiliate.rentals.gydi.payment.domain.port.out.PaymentRepositoryPort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Adapter implementing PaymentRepositoryPort using JPA.
 * <p>
 * Translates between domain model (PaymentBooking) and persistence model (PaymentBookingEntity).
 * </p>
 */
@Component
public class PaymentRepositoryAdapter implements PaymentRepositoryPort {

    private final PaymentJpaRepository jpaRepository;

    public PaymentRepositoryAdapter(PaymentJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public PaymentBooking save(PaymentBooking payment) {
        PaymentBookingEntity entity = toEntity(payment);
        PaymentBookingEntity savedEntity = jpaRepository.save(entity);
        return toDomain(savedEntity);
    }

    @Override
    public Optional<PaymentBooking> findById(Long id) {
        return jpaRepository.findById(id)
            .map(this::toDomain);
    }

    @Override
    public Optional<PaymentBooking> findByBookingId(Long bookingId) {
        return jpaRepository.findByBookingId(bookingId)
            .map(this::toDomain);
    }

    @Override
    public List<PaymentBooking> findByUserId(Long userId) {
        return jpaRepository.findByUserId(userId)
            .stream()
            .map(this::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<PaymentBooking> findByReferralLinkId(Long referralLinkId) {
        return jpaRepository.findByReferralLinkId(referralLinkId)
            .stream()
            .map(this::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<PaymentBooking> findByStatus(String status) {
        PaymentBookingEntity.PaymentStatusEntity statusEntity =
            PaymentBookingEntity.PaymentStatusEntity.valueOf(status);

        return jpaRepository.findByStatus(statusEntity)
            .stream()
            .map(this::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public boolean existsByBookingId(Long bookingId) {
        return jpaRepository.existsByBookingId(bookingId);
    }

    // ========== MAPPING: Entity ↔ Domain ==========

    /**
     * Converts domain model to JPA entity.
     */
    private PaymentBookingEntity toEntity(PaymentBooking payment) {
        PaymentBookingEntity entity = PaymentBookingEntity.create()
            .bookingId(payment.getBookingId())
            .referralLinkId(payment.getReferralLinkId())
            .userId(payment.getUserId())
            .totalAmount(payment.getTotalAmount().getAmount())
            .commissionAmount(payment.getCommissionAmount().getAmount())
            .commissionPercentage(payment.getCommissionPercentage())
            .currency(payment.getTotalAmount().getCurrencyCode())
            .status(toEntityStatus(payment.getStatus()))
            .paidAt(payment.getPaidAt())
            .failureReason(payment.getFailureReason());

        // Set ID if exists (for updates)
        if (payment.getId() != null) {
            entity.setId(payment.getId());
        }

        return entity;
    }

    /**
     * Converts JPA entity to domain model.
     */
    private PaymentBooking toDomain(PaymentBookingEntity entity) {
        Money totalAmount = Money.of(entity.getTotalAmount(), entity.getCurrency());
        Money commissionAmount = Money.of(entity.getCommissionAmount(), entity.getCurrency());

        return PaymentBooking.reconstitute(
            entity.getId(),
            entity.getBookingId(),
            entity.getReferralLinkId(),
            entity.getUserId(),
            totalAmount,
            commissionAmount,
            entity.getCommissionPercentage(),
            toDomainStatus(entity.getStatus()),
            entity.getPaidAt(),
            entity.getFailureReason(),
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }

    /**
     * Converts domain PaymentStatus to entity PaymentStatusEntity.
     */
    private PaymentBookingEntity.PaymentStatusEntity toEntityStatus(PaymentStatus domainStatus) {
        return PaymentBookingEntity.PaymentStatusEntity.valueOf(domainStatus.name());
    }

    /**
     * Converts entity PaymentStatusEntity to domain PaymentStatus.
     */
    private PaymentStatus toDomainStatus(PaymentBookingEntity.PaymentStatusEntity entityStatus) {
        return PaymentStatus.valueOf(entityStatus.name());
    }
}
