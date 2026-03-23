package com.affiliate.rentals.gydi.commissions.application.usecase;

import com.affiliate.rentals.gydi.commissions.domain.model.ReferralCommission;
import com.affiliate.rentals.gydi.commissions.domain.model.ReferralCommissionStatus;
import com.affiliate.rentals.gydi.commissions.domain.ports.ReferralCommissionRepositoryPort;

import lombok.extern.slf4j.Slf4j;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Use Case: Approves all PENDING affiliate commissions for a given affiliate.
 * <p>
 * Triggered when an affiliate completes Stripe Connect onboarding (payouts
 * enabled).
 * Scans all PENDING commissions for that affiliate and transitions eligible
 * ones
 * to APPROVED so they are picked up by the biweekly payment scheduler.
 * </p>
 * <p>
 * A commission is "eligible" when:
 * <ul>
 * <li>Status is PENDING (host charge already succeeded, affiliate had no
 * Connect account)</li>
 * <li>The 7-day dispute period has ended</li>
 * </ul>
 * If the dispute period is still active the commission stays in PENDING — the
 * existing
 * {@code findCommissionsReadyForApproval} path in
 * {@link ApproveReferralCommissionUseCase}
 * will handle it once the period expires.
 * </p>
 * <p>
 * This use case delegates per-commission state transitions to
 * {@link ApproveReferralCommissionUseCase} to keep approval logic in a single
 * place
 * and avoid duplication.
 * </p>
 */
@Slf4j
@Service
public class ApprovePendingCommissionsForAffiliateUseCase {

    private final ReferralCommissionRepositoryPort referralCommissionRepository;
    private final ApproveReferralCommissionUseCase approveReferralCommissionUseCase;

    public ApprovePendingCommissionsForAffiliateUseCase(
            ReferralCommissionRepositoryPort referralCommissionRepository,
            ApproveReferralCommissionUseCase approveReferralCommissionUseCase) {
        this.referralCommissionRepository = referralCommissionRepository;
        this.approveReferralCommissionUseCase = approveReferralCommissionUseCase;
    }

    /**
     * Approves all eligible PENDING commissions for the given affiliate.
     *
     * @param affiliateId the user ID of the affiliate who just completed onboarding
     * @return the number of commissions that were transitioned to APPROVED
     */
    @Transactional
    public int execute(Long affiliateId) {
        List<ReferralCommission> pending = referralCommissionRepository
                .findByAffiliateIdAndStatus(affiliateId, ReferralCommissionStatus.PENDING);

        if (pending.isEmpty()) {
            log.debug("No PENDING commissions found for affiliate {}", affiliateId);
            return 0;
        }

        log.info("Found {} PENDING commission(s) for affiliate {} — attempting approval",
                pending.size(), affiliateId);

        int approved = 0;

        for (ReferralCommission commission : pending) {
            try {
                if (commission.isReadyForApproval()) {
                    approveReferralCommissionUseCase.execute(commission.getId());
                    approved++;
                    log.info("Commission {} approved for affiliate {} (dispute period ended)",
                            commission.getId(), affiliateId);
                } else {
                    log.debug(
                            "Commission {} for affiliate {} is PENDING but dispute period still active "
                                    + "(ends {}). Skipping — will be approved when dispute period expires.",
                            commission.getId(), affiliateId,
                            commission.getDisputePeriod().getDisputePeriodEndsAt());
                }
            } catch (Exception e) {
                log.error("Error approving commission {} for affiliate {}: {}",
                        commission.getId(), affiliateId, e.getMessage(), e);
            }
        }

        log.info("Approved {}/{} PENDING commissions for affiliate {}",
                approved, pending.size(), affiliateId);

        return approved;
    }
}
