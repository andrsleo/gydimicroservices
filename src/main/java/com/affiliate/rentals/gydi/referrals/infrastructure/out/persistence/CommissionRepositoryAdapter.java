package com.affiliate.rentals.gydi.referrals.infrastructure.out.persistence;

import com.affiliate.rentals.gydi.referrals.domain.model.Commission;
import com.affiliate.rentals.gydi.referrals.domain.model.CommissionStatus;
import com.affiliate.rentals.gydi.referrals.domain.port.CommissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Adapter que implementa CommissionRepository usando JPA
 */
@Component
@RequiredArgsConstructor
public class CommissionRepositoryAdapter implements CommissionRepository {

    private final CommissionJpaRepository jpaRepository;

    @Override
    public Commission save(Commission commission) {
        CommissionJpaEntity entity = toEntity(commission);
        CommissionJpaEntity saved = jpaRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Commission updateStatus(Long commissionId, CommissionStatus newStatus) {
        CommissionJpaEntity entity = jpaRepository.findById(commissionId)
            .orElseThrow(() -> new IllegalArgumentException("Commission not found: " + commissionId));

        entity.setStatus(newStatus);
        CommissionJpaEntity updated = jpaRepository.save(entity);
        return toDomain(updated);
    }

    @Override
    public Optional<Commission> findById(Long id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public List<Commission> findByBookingId(Long bookingId) {
        return jpaRepository.findByBookingId(bookingId).stream()
            .map(this::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<Commission> findReadyForApproval(LocalDateTime beforeDate) {
        return jpaRepository.findReadyForApproval(beforeDate).stream()
            .map(this::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<Commission> findApprovedForPayout() {
        return jpaRepository.findApprovedForPayout().stream()
            .map(this::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<Commission> findByStatus(CommissionStatus status) {
        return jpaRepository.findByStatus(status).stream()
            .map(this::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<Commission> findByAffiliateId(Long affiliateId) {
        return jpaRepository.findByAffiliateId(affiliateId).stream()
            .map(this::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public BigDecimal calculateTotalEarningsByAffiliateId(Long affiliateId) {
        return jpaRepository.calculateTotalEarningsByAffiliateId(affiliateId);
    }

    @Override
    public BigDecimal calculateEarningsByAffiliateIdAndStatus(Long affiliateId, CommissionStatus status) {
        return jpaRepository.calculateEarningsByAffiliateIdAndStatus(affiliateId, status.name());
    }

    @Override
    public long countByAffiliateIdAndStatus(Long affiliateId, CommissionStatus status) {
        return jpaRepository.countByAffiliateIdAndStatus(affiliateId, status.name());
    }

    // Mappers
    private CommissionJpaEntity toEntity(Commission domain) {
        CommissionJpaEntity entity = new CommissionJpaEntity();
        entity.setId(domain.getId());
        entity.setBookingId(domain.getBookingId());
        entity.setCommissionRate(domain.getCommissionRate());
        entity.setCommissionAmount(domain.getCommissionAmount());
        entity.setAffiliatePlan(domain.getAffiliatePlan());
        entity.setStatus(domain.getStatus());
        entity.setVerificationHash(domain.getVerificationHash());
        entity.setCreatedAt(domain.getCreatedAt());
        return entity;
    }

    private Commission toDomain(CommissionJpaEntity entity) {
        Commission domain = Commission.create(
            entity.getBookingId(),
            entity.getCommissionAmount(),
            entity.getCommissionRate(),
            entity.getAffiliatePlan()
        );
        domain.setId(entity.getId());
        domain.setStatus(entity.getStatus());
        domain.setVerificationHash(entity.getVerificationHash());
        domain.setCreatedAt(entity.getCreatedAt());
        return domain;
    }
}