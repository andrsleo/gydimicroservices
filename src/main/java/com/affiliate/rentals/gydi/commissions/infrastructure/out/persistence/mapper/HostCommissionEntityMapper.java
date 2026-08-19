package com.affiliate.rentals.gydi.commissions.infrastructure.out.persistence.mapper;

import com.affiliate.rentals.gydi.commissions.domain.model.HostCommission;
import com.affiliate.rentals.gydi.commissions.domain.model.HostCommissionStatus;
import com.affiliate.rentals.gydi.commissions.domain.model.vo.CommissionAmount;
import com.affiliate.rentals.gydi.commissions.infrastructure.out.persistence.entity.HostCommissionJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class HostCommissionEntityMapper {

    public HostCommission toDomain(HostCommissionJpaEntity entity) {
        if (entity == null) return null;

        CommissionAmount amount = CommissionAmount.reconstitute(
            entity.getBookingAmount(),
            entity.getCommissionRate(),
            entity.getCommissionAmount(),
            entity.getCurrency()
        );

        return HostCommission.reconstitute(
            entity.getId(),
            entity.getBookingId(),
            entity.getHostId(),
            entity.getHostPlan(),
            amount,
            entity.getStripePaymentIntentId(),
            entity.getStripeChargeId(),
            HostCommissionStatus.valueOf(entity.getStatus()),
            entity.getChargedAt(),
            entity.getFailureReason(),
            entity.getAttemptCount(),
            entity.getLastAttemptAt(),
            entity.getNextRetryAt(),
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }

    public HostCommissionJpaEntity toEntity(HostCommission domain) {
        if (domain == null) return null;

        HostCommissionJpaEntity entity = new HostCommissionJpaEntity();
        entity.setId(domain.getId());
        entity.setBookingId(domain.getBookingId());
        entity.setHostId(domain.getHostId());
        entity.setHostPlan(domain.getHostPlan());
        entity.setBookingAmount(domain.getAmount().getBookingAmount());
        entity.setCommissionRate(domain.getAmount().getCommissionRate());
        entity.setCommissionAmount(domain.getAmount().getCommissionAmount());
        entity.setCurrency(domain.getAmount().getCurrency());
        entity.setStripePaymentIntentId(domain.getStripePaymentIntentId());
        entity.setStripeChargeId(domain.getStripeChargeId());
        entity.setStatus(domain.getStatus().name());
        entity.setChargedAt(domain.getChargedAt());
        entity.setFailureReason(domain.getFailureReason());
        entity.setAttemptCount(domain.getAttemptCount());
        entity.setLastAttemptAt(domain.getLastAttemptAt());
        entity.setNextRetryAt(domain.getNextRetryAt());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(domain.getUpdatedAt());
        return entity;
    }
}
