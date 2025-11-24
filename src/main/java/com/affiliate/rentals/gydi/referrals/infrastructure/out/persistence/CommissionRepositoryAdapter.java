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
    public List<Commission> findByAffiliateId(Long affiliateId) {
        return jpaRepository.findByAffiliateId(affiliateId).stream()
            .map(this::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<Commission> findByAffiliateIdAndStatus(Long affiliateId, CommissionStatus status) {
        return jpaRepository.findByAffiliateIdAndStatus(affiliateId, status).stream()
            .map(this::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<Commission> findByReferralLinkId(Long referralLinkId) {
        return jpaRepository.findByReferralLinkId(referralLinkId).stream()
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
    public List<Commission> findByAffiliateIdAndDateRange(Long affiliateId,
                                                           LocalDateTime from,
                                                           LocalDateTime to) {
        return jpaRepository.findByAffiliateIdAndDateRange(affiliateId, from, to).stream()
            .map(this::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public BigDecimal calculateTotalEarnings(Long affiliateId) {
        return jpaRepository.calculateTotalEarnings(affiliateId);
    }

    @Override
    public BigDecimal calculateEarningsByStatus(Long affiliateId, CommissionStatus status) {
        return jpaRepository.calculateEarningsByStatus(affiliateId, status);
    }

    @Override
    public long countByAffiliateIdAndStatus(Long affiliateId, CommissionStatus status) {
        return jpaRepository.countByAffiliateIdAndStatus(affiliateId, status);
    }

    @Override
    public Map<String, BigDecimal> calculateMonthlyEarnings(Long affiliateId, int year) {
        LocalDateTime startOfYear = LocalDateTime.of(year, 1, 1, 0, 0);
        LocalDateTime endOfYear = LocalDateTime.of(year, 12, 31, 23, 59, 59);

        List<CommissionJpaEntity> commissions = jpaRepository.findByAffiliateIdAndDateRange(
            affiliateId, startOfYear, endOfYear);

        Map<String, BigDecimal> monthlyEarnings = new HashMap<>();
        DateTimeFormatter monthFormatter = DateTimeFormatter.ofPattern("yyyy-MM");

        for (CommissionJpaEntity commission : commissions) {
            String monthKey = commission.getCreatedAt().format(monthFormatter);
            BigDecimal currentAmount = monthlyEarnings.getOrDefault(monthKey, BigDecimal.ZERO);
            monthlyEarnings.put(monthKey, currentAmount.add(commission.getCommissionAmount()));
        }

        return monthlyEarnings;
    }

    // Mappers
    private CommissionJpaEntity toEntity(Commission domain) {
        CommissionJpaEntity entity = new CommissionJpaEntity();
        entity.setId(domain.getId());
        entity.setReferralLinkId(domain.getReferralLinkId());
        entity.setAffiliateId(domain.getAffiliateId());
        entity.setPropertyId(domain.getPropertyId());
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
            entity.getReferralLinkId(),
            entity.getAffiliateId(),
            entity.getPropertyId(),
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