package com.affiliate.rentals.gydi.subscriptions.application.usecase;

import com.affiliate.rentals.gydi.subscriptions.application.dto.CommissionRatesResponse;
import com.affiliate.rentals.gydi.subscriptions.application.dto.CommissionRatesResponse.AffiliateRateInfo;
import com.affiliate.rentals.gydi.subscriptions.application.dto.CommissionRatesResponse.HostRateInfo;
import com.affiliate.rentals.gydi.subscriptions.domain.model.Plan;
import com.affiliate.rentals.gydi.subscriptions.domain.ports.PlanRepositoryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Use case for retrieving commission rates for all subscription plans.
 *
 * <p>This service provides a complete view of commission rates for both affiliates
 * and hosts across all available subscription tiers. This is useful for:
 * <ul>
 *   <li>Displaying commission rates in the UI</li>
 *   <li>Plan comparison tables</li>
 *   <li>Commission calculators</li>
 * </ul>
 *
 * @author GYDI Development Team
 * @version 2.0
 * @since 2026-02-04
 */
@Service
public class GetCommissionRatesUseCase {

    private static final Logger logger = LoggerFactory.getLogger(GetCommissionRatesUseCase.class);

    private final PlanRepositoryPort planRepository;

    public GetCommissionRatesUseCase(PlanRepositoryPort planRepository) {
        this.planRepository = planRepository;
    }

    /**
     * Executes the get commission rates use case.
     *
     * <p>Retrieves all active plans and extracts their commission rates,
     * organizing them by role (affiliate vs host).</p>
     *
     * @return Commission rates for all plans, organized by role
     */
    @Transactional(readOnly = true)
    public CommissionRatesResponse execute() {
        logger.debug("Retrieving commission rates for all active plans");

        List<Plan> activePlans = planRepository.findAllActive();

        Map<String, AffiliateRateInfo> affiliateRates = new HashMap<>();
        Map<String, HostRateInfo> hostRates = new HashMap<>();

        for (Plan plan : activePlans) {
            String planCode = plan.planCode();

            // Extract affiliate commission rate
            BigDecimal affiliateRate = extractAffiliateRate(plan);
            affiliateRates.put(planCode, AffiliateRateInfo.of(planCode, affiliateRate));

            // Extract host commission rate
            BigDecimal hostRate = extractHostRate(plan);
            hostRates.put(planCode, HostRateInfo.of(planCode, hostRate));

            logger.debug("Plan {}: Affiliate {}%, Host {}%",
                    planCode,
                    affiliateRate.multiply(new BigDecimal("100")),
                    hostRate.multiply(new BigDecimal("100")));
        }

        logger.info("Retrieved commission rates for {} plans", activePlans.size());

        return new CommissionRatesResponse(affiliateRates, hostRates);
    }

    /**
     * Extracts affiliate commission rate from plan with fallback logic.
     *
     * @param plan The subscription plan
     * @return Affiliate commission rate
     */
    private BigDecimal extractAffiliateRate(Plan plan) {
        // Use new field if available
        if (plan.affiliateCommissionRate() != null) {
            return plan.affiliateCommissionRate();
        }

        // Fallback to legacy commission_rate field
        if (plan.commissionRate() != null) {
            return plan.commissionRate();
        }

        // Fallback to hard-coded defaults (should not happen if DB is properly migrated)
        return getDefaultAffiliateRate(plan.planCode());
    }

    /**
     * Extracts host commission rate from plan with fallback logic.
     *
     * @param plan The subscription plan
     * @return Host commission rate (platform fee rate)
     */
    private BigDecimal extractHostRate(Plan plan) {
        // Use new field if available
        if (plan.hostCommissionRate() != null) {
            return plan.hostCommissionRate();
        }

        // Fallback to hard-coded defaults (should not happen if DB is properly migrated)
        return getDefaultHostRate(plan.planCode());
    }

    /**
     * Returns default affiliate commission rate for a plan code.
     *
     * @param planCode Plan code (FREE, PRO, ELITE)
     * @return Default affiliate rate
     */
    private BigDecimal getDefaultAffiliateRate(String planCode) {
        return switch (planCode) {
            case "FREE" -> new BigDecimal("0.02");  // 2%
            case "PRO" -> new BigDecimal("0.05");   // 5%
            case "ELITE" -> new BigDecimal("0.10"); // 10%
            default -> {
                logger.warn("Unknown plan code for affiliate rate: {}, defaulting to 0.02", planCode);
                yield new BigDecimal("0.02");
            }
        };
    }

    /**
     * Returns default host commission rate for a plan code.
     *
     * @param planCode Plan code (FREE, PRO, ELITE)
     * @return Default host rate
     */
    private BigDecimal getDefaultHostRate(String planCode) {
        return switch (planCode) {
            case "FREE" -> new BigDecimal("0.25");  // 25%
            case "PRO" -> new BigDecimal("0.20");   // 20%
            case "ELITE" -> new BigDecimal("0.15"); // 15%
            default -> {
                logger.warn("Unknown plan code for host rate: {}, defaulting to 0.25", planCode);
                yield new BigDecimal("0.25");
            }
        };
    }
}
