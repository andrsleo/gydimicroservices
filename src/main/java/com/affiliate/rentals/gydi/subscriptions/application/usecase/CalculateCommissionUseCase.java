package com.affiliate.rentals.gydi.subscriptions.application.usecase;

import com.affiliate.rentals.gydi.subscriptions.application.dto.CalculateCommissionRequest;
import com.affiliate.rentals.gydi.subscriptions.application.dto.CommissionBreakdownResponse;
import com.affiliate.rentals.gydi.subscriptions.domain.exception.PlanNotFoundException;
import com.affiliate.rentals.gydi.subscriptions.domain.model.Plan;
import com.affiliate.rentals.gydi.subscriptions.domain.ports.PlanRepositoryPort;
import com.affiliate.rentals.gydi.subscriptions.domain.service.CommissionCalculatorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Use case for calculating commission breakdown for a booking transaction.
 *
 * <p>This service calculates how commission is distributed between:
 * <ul>
 *   <li><strong>Platform Fee</strong>: Amount platform CHARGES to host</li>
 *   <li><strong>Affiliate Commission</strong>: Amount platform PAYS to affiliate</li>
 *   <li><strong>Host Net Income</strong>: Amount host receives</li>
 *   <li><strong>Platform Profit</strong>: Net amount platform earns</li>
 * </ul>
 *
 * <p>The calculation is based on the subscription plans of both the host and affiliate.</p>
 *
 * @author GYDI Development Team
 * @version 2.0
 * @since 2026-02-04
 */
@Service
public class CalculateCommissionUseCase {

    private static final Logger logger = LoggerFactory.getLogger(CalculateCommissionUseCase.class);

    private final PlanRepositoryPort planRepository;
    private final CommissionCalculatorService commissionCalculator;

    public CalculateCommissionUseCase(
            PlanRepositoryPort planRepository,
            CommissionCalculatorService commissionCalculator
    ) {
        this.planRepository = planRepository;
        this.commissionCalculator = commissionCalculator;
    }

    /**
     * Executes the calculate commission use case.
     *
     * <p><strong>Business Logic:</strong></p>
     * <ol>
     *   <li>Retrieve host's subscription plan from database</li>
     *   <li>Retrieve affiliate's subscription plan from database</li>
     *   <li>Extract commission rates for both roles</li>
     *   <li>Calculate complete commission breakdown using domain service</li>
     *   <li>Return breakdown as DTO</li>
     * </ol>
     *
     * @param request The calculation request containing booking amount and plan codes
     * @return Commission breakdown with all calculated amounts
     * @throws PlanNotFoundException if host or affiliate plan is not found
     * @throws IllegalArgumentException if request validation fails
     */
    @Transactional(readOnly = true)
    public CommissionBreakdownResponse execute(CalculateCommissionRequest request) {
        logger.debug("Calculating commission for booking: {}, host plan: {}, affiliate plan: {}",
                request.bookingAmount(), request.hostPlan(), request.affiliatePlan());

        // 1. Retrieve host plan
        Plan hostPlan = planRepository.findByPlanCode(request.hostPlan())
                .orElseThrow(() -> new PlanNotFoundException(
                        "Host plan not found: " + request.hostPlan()
                ));

        // 2. Retrieve affiliate plan
        Plan affiliatePlan = planRepository.findByPlanCode(request.affiliatePlan())
                .orElseThrow(() -> new PlanNotFoundException(
                        "Affiliate plan not found: " + request.affiliatePlan()
                ));

        // 3. Extract commission rates
        BigDecimal hostCommissionRate = getHostCommissionRate(hostPlan);
        BigDecimal affiliateCommissionRate = getAffiliateCommissionRate(affiliatePlan);

        logger.debug("Using rates - Host: {}%, Affiliate: {}%",
                hostCommissionRate.multiply(new BigDecimal("100")),
                affiliateCommissionRate.multiply(new BigDecimal("100")));

        // 4. Calculate commission breakdown
        CommissionCalculatorService.CommissionBreakdown breakdown =
                commissionCalculator.calculateBreakdown(
                        request.bookingAmount(),
                        hostCommissionRate,
                        affiliateCommissionRate
                );

        // 5. Map to response DTO
        CommissionBreakdownResponse response = CommissionBreakdownResponse.of(
                breakdown.bookingAmount(),
                breakdown.platformFee(),
                breakdown.affiliateCommission(),
                breakdown.hostNetIncome(),
                breakdown.platformProfit(),
                request.hostPlan(),
                request.affiliatePlan()
        );

        logger.info("Commission calculated - Platform fee: {}, Affiliate commission: {}, Platform profit: {}",
                response.platformFee(), response.affiliateCommission(), response.platformProfit());

        return response;
    }

    /**
     * Extracts host commission rate from plan.
     * Uses new role-specific field if available, falls back to legacy field.
     *
     * @param plan Host's subscription plan
     * @return Host commission rate (platform fee rate)
     */
    private BigDecimal getHostCommissionRate(Plan plan) {
        if (plan.hostCommissionRate() != null) {
            return plan.hostCommissionRate();
        }

        // Fallback to default rates if not set
        return switch (plan.planCode()) {
            case "FREE" -> new BigDecimal("0.25");  // 25%
            case "PRO" -> new BigDecimal("0.20");   // 20%
            case "ELITE" -> new BigDecimal("0.15"); // 15%
            default -> throw new IllegalArgumentException("Unknown plan code: " + plan.planCode());
        };
    }

    /**
     * Extracts affiliate commission rate from plan.
     * Uses new role-specific field if available, falls back to legacy field.
     *
     * @param plan Affiliate's subscription plan
     * @return Affiliate commission rate
     */
    private BigDecimal getAffiliateCommissionRate(Plan plan) {
        if (plan.affiliateCommissionRate() != null) {
            return plan.affiliateCommissionRate();
        }

        // Fallback to legacy commission_rate field or defaults
        if (plan.commissionRate() != null) {
            return plan.commissionRate();
        }

        // Fallback to default rates if not set
        return switch (plan.planCode()) {
            case "FREE" -> new BigDecimal("0.02");  // 2%
            case "PRO" -> new BigDecimal("0.05");   // 5%
            case "ELITE" -> new BigDecimal("0.10"); // 10%
            default -> throw new IllegalArgumentException("Unknown plan code: " + plan.planCode());
        };
    }
}
