package com.affiliate.rentals.gydi.commissions.infrastructure.out.persistence.mapper;

import com.affiliate.rentals.gydi.commissions.domain.model.ReferralCommission;
import com.affiliate.rentals.gydi.commissions.domain.model.ReferralCommissionStatus;
import com.affiliate.rentals.gydi.commissions.domain.model.vo.CommissionAmount;
import com.affiliate.rentals.gydi.commissions.domain.model.vo.DisputePeriod;
import com.affiliate.rentals.gydi.commissions.domain.model.vo.PaymentSchedule;
import com.affiliate.rentals.gydi.commissions.infrastructure.out.persistence.entity.ReferralCommissionJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class ReferralCommissionEntityMapper {

    public ReferralCommission toDomain(ReferralCommissionJpaEntity entity) {
        if (entity == null) return null;

        CommissionAmount amount = CommissionAmount.reconstitute(
            entity.getBookingAmount(),
            entity.getCommissionRate(),
            entity.getCommissionAmount(),
            entity.getCurrency()
        );

        PaymentSchedule paymentSchedule = PaymentSchedule.reconstitute(entity.getScheduledPaymentDate());
        DisputePeriod disputePeriod = DisputePeriod.reconstitute(entity.getDisputePeriodEndsAt());

        return ReferralCommission.reconstitute(
            entity.getId(),
            entity.getBookingId(),
            entity.getAffiliateId(),
            entity.getAffiliatePlan(),
            amount,
            paymentSchedule,
            disputePeriod,
            entity.getPaypalPayoutBatchId(),
            entity.getPaypalPayoutItemId(),
            entity.getStripeTransferId(),
            entity.getStripePayoutId(),
            ReferralCommissionStatus.valueOf(entity.getStatus()),
            entity.getPaidAt(),
            entity.getFailureReason(),
            entity.getAttemptCount(),
            entity.getLastAttemptAt(),
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }

    public ReferralCommissionJpaEntity toEntity(ReferralCommission domain) {
        if (domain == null) return null;

        ReferralCommissionJpaEntity entity = new ReferralCommissionJpaEntity();
        entity.setId(domain.getId());
        entity.setBookingId(domain.getBookingId());
        entity.setAffiliateId(domain.getAffiliateId());
        entity.setAffiliatePlan(domain.getAffiliatePlan());
        entity.setBookingAmount(domain.getAmount().getBookingAmount());
        entity.setCommissionRate(domain.getAmount().getCommissionRate());
        entity.setCommissionAmount(domain.getAmount().getCommissionAmount());
        entity.setCurrency(domain.getAmount().getCurrency());
        entity.setScheduledPaymentDate(domain.getPaymentSchedule().getScheduledPaymentDate());
        entity.setDisputePeriodEndsAt(domain.getDisputePeriod().getDisputePeriodEndsAt());
        entity.setPaypalPayoutBatchId(domain.getPaypalPayoutBatchId());
        entity.setPaypalPayoutItemId(domain.getPaypalPayoutItemId());
        entity.setStripeTransferId(domain.getStripeTransferId());
        entity.setStripePayoutId(domain.getStripePayoutId());
        entity.setStatus(domain.getStatus().name());
        entity.setPaidAt(domain.getPaidAt());
        entity.setFailureReason(domain.getFailureReason());
        entity.setAttemptCount(domain.getAttemptCount());
        entity.setLastAttemptAt(domain.getLastAttemptAt());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(domain.getUpdatedAt());
        return entity;
    }
}
