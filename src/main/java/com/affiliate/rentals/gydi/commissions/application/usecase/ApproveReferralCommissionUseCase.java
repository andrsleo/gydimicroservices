package com.affiliate.rentals.gydi.commissions.application.usecase;

import com.affiliate.rentals.gydi.commissions.domain.exception.CommissionNotFoundException;
import com.affiliate.rentals.gydi.commissions.domain.model.ReferralCommission;
import com.affiliate.rentals.gydi.commissions.domain.ports.ReferralCommissionRepositoryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ApproveReferralCommissionUseCase {
    private static final Logger logger = LoggerFactory.getLogger(ApproveReferralCommissionUseCase.class);
    private final ReferralCommissionRepositoryPort repository;

    public ApproveReferralCommissionUseCase(ReferralCommissionRepositoryPort repository) {
        this.repository = repository;
    }

    @Transactional
    public void execute(Long commissionId) {
        ReferralCommission commission = repository.findById(commissionId)
            .orElseThrow(() -> CommissionNotFoundException.forId(commissionId));

        if (commission.isReadyForApproval()) {
            commission.approve();
            repository.save(commission);
            logger.info("Approved referral commission ID: {}, scheduled for payment on: {}",
                commissionId, commission.getPaymentSchedule().getScheduledPaymentDate());
        } else {
            logger.warn("Commission ID: {} not ready for approval. Status: {}, dispute period active: {}",
                commissionId, commission.getStatus(), commission.isDisputePeriodActive());
        }
    }
}
