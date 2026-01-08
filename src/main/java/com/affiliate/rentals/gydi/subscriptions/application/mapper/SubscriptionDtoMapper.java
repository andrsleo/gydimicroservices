package com.affiliate.rentals.gydi.subscriptions.application.mapper;

import com.affiliate.rentals.gydi.subscriptions.application.dto.*;
import com.affiliate.rentals.gydi.subscriptions.domain.model.*;
import org.springframework.stereotype.Component;

/**
 * Manual mapper for converting between subscription domain models and DTOs.
 *
 * <p>This mapper provides mapping between domain models and application layer DTOs,
 * maintaining type safety and clean separation of concerns.</p>
 *
 * @author GYDI Development Team
 */
@Component
public class SubscriptionDtoMapper {

    /**
     * Maps a Plan domain model to a PlanResponse DTO.
     *
     * @param plan the domain plan
     * @return the corresponding PlanResponse DTO
     */
    public PlanResponse toPlanResponse(Plan plan) {
        if (plan == null) {
            return null;
        }

        return new PlanResponse(
                plan.id(),
                plan.planCode(),
                plan.planName(),
                plan.description(),
                plan.monthlyPrice(),
                plan.currency(),
                plan.commissionRate(),
                plan.referralLimitPerMonth(),
                plan.propertyPublishLimit(),
                plan.isActive(),
                plan.isFeatured(),
                plan.displayOrder(),
                plan.createdAt(),
                plan.updatedAt()
        );
    }

    /**
     * Maps a UserSubscription domain model to a UserSubscriptionResponse DTO.
     *
     * @param subscription the domain subscription
     * @param plan the related plan (for including plan details in response)
     * @return the corresponding UserSubscriptionResponse DTO
     */
    public UserSubscriptionResponse toUserSubscriptionResponse(UserSubscription subscription, Plan plan) {
        if (subscription == null) {
            return null;
        }

        return new UserSubscriptionResponse(
                subscription.id(),
                subscription.userId(),
                subscription.planId(),
                plan != null ? plan.planCode() : null,
                plan != null ? plan.planName() : null,
                subscription.status() != null ? subscription.status().name() : null,
                subscription.startedAt(),
                subscription.expiresAt(),
                subscription.canceledAt(),
                subscription.cancelationReason(),
                subscription.autoRenew(),
                subscription.lastRenewalAt(),
                subscription.nextBillingDate(),
                subscription.stripeSubscriptionId(),
                subscription.createdAt(),
                subscription.updatedAt()
        );
    }

    /**
     * Maps a PaymentMethod domain model to a PaymentMethodResponse DTO.
     *
     * @param paymentMethod the domain payment method
     * @return the corresponding PaymentMethodResponse DTO
     */
    public PaymentMethodResponse toPaymentMethodResponse(PaymentMethod paymentMethod) {
        if (paymentMethod == null) {
            return null;
        }

        return new PaymentMethodResponse(
                paymentMethod.id(),
                paymentMethod.userId(),
                paymentMethod.methodType() != null ? paymentMethod.methodType().name() : null,
                paymentMethod.cardLastFour(),
                paymentMethod.cardBrand(),
                paymentMethod.cardExpMonth(),
                paymentMethod.cardExpYear(),
                paymentMethod.billingEmail(),
                paymentMethod.isDefault(),
                paymentMethod.isActive(),
                paymentMethod.isExpired(),
                paymentMethod.getDisplayName(),
                paymentMethod.createdAt()
        );
    }

    /**
     * Maps a SubscriptionTransaction domain model to a SubscriptionTransactionResponse DTO.
     *
     * @param transaction the domain transaction
     * @param fromPlan the plan being changed from (for plan changes, can be null)
     * @param toPlan the plan being changed to (can be null)
     * @return the corresponding SubscriptionTransactionResponse DTO
     */
    public SubscriptionTransactionResponse toTransactionResponse(
            SubscriptionTransaction transaction,
            Plan fromPlan,
            Plan toPlan) {
        if (transaction == null) {
            return null;
        }

        return new SubscriptionTransactionResponse(
                transaction.id(),
                transaction.userSubscriptionId(),
                transaction.transactionType() != null ? transaction.transactionType().name() : null,
                transaction.transactionStatus() != null ? transaction.transactionStatus().name() : null,
                transaction.fromPlanId(),
                fromPlan != null ? fromPlan.planName() : null,
                transaction.toPlanId(),
                toPlan != null ? toPlan.planName() : null,
                transaction.amount(),
                transaction.currency(),
                transaction.stripePaymentIntentId(),
                transaction.stripeChargeId(),
                transaction.failureReason(),
                transaction.processedAt(),
                transaction.createdAt()
        );
    }

    /**
     * Convenience method for transaction response without plan details.
     */
    public SubscriptionTransactionResponse toTransactionResponse(SubscriptionTransaction transaction) {
        return toTransactionResponse(transaction, null, null);
    }
}
