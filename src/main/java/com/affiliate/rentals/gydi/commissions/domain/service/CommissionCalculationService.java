package com.affiliate.rentals.gydi.commissions.domain.service;

import com.affiliate.rentals.gydi.commissions.domain.exception.CommissionCalculationException;
import com.affiliate.rentals.gydi.commissions.domain.model.ReferralCommission;
import com.affiliate.rentals.gydi.commissions.domain.model.HostCommission;
import com.affiliate.rentals.gydi.commissions.domain.model.vo.CommissionAmount;
import com.affiliate.rentals.gydi.commissions.domain.ports.UserSubscriptionPort;
import com.affiliate.rentals.gydi.subscriptions.domain.service.CommissionCalculatorService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Domain Service for commission calculation orchestration.
 * <p>
 * This service coordinates commission creation using:
 * - {@link CommissionCalculatorService} for rate calculations (from
 * subscriptions bounded context)
 * - {@link UserSubscriptionPort} for fetching user plan data
 * </p>
 * <p>
 * <strong>PURE DOMAIN SERVICE</strong> - No framework dependencies.
 * </p>
 */
public class CommissionCalculationService {

    private final CommissionCalculatorService calculatorService;
    private final UserSubscriptionPort userSubscriptionPort;

    public CommissionCalculationService(
            CommissionCalculatorService calculatorService,
            UserSubscriptionPort userSubscriptionPort) {
        this.calculatorService = Objects.requireNonNull(
                calculatorService,
                "Calculator service cannot be null");
        this.userSubscriptionPort = Objects.requireNonNull(
                userSubscriptionPort,
                "User subscription port cannot be null");
    }

    /**
     * Calculates and creates host commission for a finished booking.
     *
     * @param bookingId     booking ID
     * @param hostId        host user ID
     * @param bookingAmount total booking amount
     * @param currency      currency code
     * @return HostCommission aggregate
     * @throws CommissionCalculationException if calculation fails
     */
    /**
     * Calculates and creates host commission using a specific rate (e.g. from
     * snapshot).
     */
    public HostCommission calculateHostCommissionWithRate(
            Long bookingId,
            Long hostId,
            BigDecimal bookingAmount,
            String currency,
            BigDecimal hostCommissionRate) {
        try {
            // Use the provided rate instead of fetching from subscription
            // However, we might still want to fetch plan code for metadata?
            // For now, let's assume we can fetch plan data just to get the code, but
            // override the rate.
            UserSubscriptionPort.UserPlanData planData = userSubscriptionPort.getUserPlanData(hostId);

            CommissionAmount amount = CommissionAmount.calculate(
                    bookingAmount,
                    hostCommissionRate, // Use provided rate
                    currency);

            return HostCommission.create(
                    bookingId,
                    hostId,
                    planData.planCode(), // Use current plan name (or should we snapshot this too? User didn't ask for
                                         // it explicitly)
                    amount);

        } catch (Exception e) {
            throw new CommissionCalculationException(
                    "Failed to calculate host commission for booking " + bookingId,
                    e);
        }
    }

    /**
     * Calculates and creates affiliate commission using a specific rate.
     */
    public ReferralCommission calculateAffiliateCommissionWithRate(
            Long bookingId,
            Long affiliateId,
            BigDecimal bookingAmount,
            String currency,
            LocalDateTime bookingFinishedAt,
            BigDecimal affiliateCommissionRate) {
        try {
            UserSubscriptionPort.UserPlanData planData = userSubscriptionPort.getUserPlanData(affiliateId);

            CommissionAmount amount = CommissionAmount.calculate(
                    bookingAmount,
                    affiliateCommissionRate, // Use provided rate
                    currency);

            return ReferralCommission.create(
                    bookingId,
                    affiliateId,
                    planData.planCode(),
                    amount,
                    bookingFinishedAt);

        } catch (Exception e) {
            throw new CommissionCalculationException(
                    "Failed to calculate affiliate commission for booking " + bookingId,
                    e);
        }
    }

    // Keep original methods for backward compatibility or direct usage
    public HostCommission calculateHostCommission(
            Long bookingId,
            Long hostId,
            BigDecimal bookingAmount,
            String currency) {
        return calculateHostCommissionWithRate(
                bookingId,
                hostId,
                bookingAmount,
                currency,
                userSubscriptionPort.getHostCommissionRate(hostId));
    }

    public ReferralCommission calculateAffiliateCommission(
            Long bookingId,
            Long affiliateId,
            BigDecimal bookingAmount,
            String currency,
            LocalDateTime bookingFinishedAt) {
        return calculateAffiliateCommissionWithRate(
                bookingId,
                affiliateId,
                bookingAmount,
                currency,
                bookingFinishedAt,
                userSubscriptionPort.getAffiliateCommissionRate(affiliateId));
    }
}
