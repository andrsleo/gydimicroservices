package com.affiliate.rentals.gydi.commissions.infrastructure.out.persistence.adapter;

import com.affiliate.rentals.gydi.commissions.domain.model.ReferralCommission;
import com.affiliate.rentals.gydi.commissions.domain.model.ReferralCommissionStatus;
import com.affiliate.rentals.gydi.commissions.domain.ports.ReferralCommissionRepositoryPort;
import com.affiliate.rentals.gydi.commissions.infrastructure.out.persistence.mapper.ReferralCommissionEntityMapper;
import com.affiliate.rentals.gydi.commissions.infrastructure.out.persistence.repository.ReferralCommissionJpaRepository;
import org.springframework.stereotype.Component;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Component
public class ReferralCommissionRepositoryAdapter implements ReferralCommissionRepositoryPort {

    private final ReferralCommissionJpaRepository jpaRepository;
    private final ReferralCommissionEntityMapper mapper;

    public ReferralCommissionRepositoryAdapter(
        ReferralCommissionJpaRepository jpaRepository,
        ReferralCommissionEntityMapper mapper
    ) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public ReferralCommission save(ReferralCommission commission) {
        var entity = mapper.toEntity(commission);
        var saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<ReferralCommission> findById(Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<ReferralCommission> findByBookingId(Long bookingId) {
        return jpaRepository.findByBookingId(bookingId).map(mapper::toDomain);
    }

    @Override
    public List<ReferralCommission> findByAffiliateId(Long affiliateId) {
        return jpaRepository.findByAffiliateId(affiliateId).stream()
            .map(mapper::toDomain)
            .toList();
    }

    @Override
    public List<ReferralCommission> findByAffiliateIdAndStatus(Long affiliateId, ReferralCommissionStatus status) {
        return jpaRepository.findByAffiliateIdAndStatus(affiliateId, status.name()).stream()
            .map(mapper::toDomain)
            .toList();
    }

    @Override
    public List<ReferralCommission> findByStatus(ReferralCommissionStatus status) {
        return jpaRepository.findByStatus(status.name()).stream()
            .map(mapper::toDomain)
            .toList();
    }

    @Override
    public List<ReferralCommission> findCommissionsReadyForApproval(LocalDateTime now) {
        return jpaRepository.findCommissionsReadyForApproval(now).stream()
            .map(mapper::toDomain)
            .toList();
    }

    @Override
    public List<ReferralCommission> findCommissionsForPayment(LocalDate paymentDate) {
        return jpaRepository.findCommissionsForPayment(paymentDate).stream()
            .map(mapper::toDomain)
            .toList();
    }

    @Override
    public boolean existsByBookingId(Long bookingId) {
        return jpaRepository.existsByBookingId(bookingId);
    }

    @Override
    public void deleteById(Long id) {
        jpaRepository.deleteById(id);
    }
}
