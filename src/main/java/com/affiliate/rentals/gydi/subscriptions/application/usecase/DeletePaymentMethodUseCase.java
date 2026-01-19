package com.affiliate.rentals.gydi.subscriptions.application.usecase;

import com.affiliate.rentals.gydi.subscriptions.domain.exception.CannotDeletePaymentMethodException;
import com.affiliate.rentals.gydi.subscriptions.domain.exception.PaymentGatewayException;
import com.affiliate.rentals.gydi.subscriptions.domain.exception.PaymentMethodNotFoundException;
import com.affiliate.rentals.gydi.subscriptions.domain.exception.PlanNotFoundException;
import com.affiliate.rentals.gydi.subscriptions.domain.model.PaymentMethod;
import com.affiliate.rentals.gydi.subscriptions.domain.model.PaymentMethodStatus;
import com.affiliate.rentals.gydi.subscriptions.domain.model.Plan;
import com.affiliate.rentals.gydi.subscriptions.domain.model.UserSubscription;
import com.affiliate.rentals.gydi.subscriptions.domain.ports.PaymentGatewayPort;
import com.affiliate.rentals.gydi.subscriptions.domain.ports.PaymentMethodRepositoryPort;
import com.affiliate.rentals.gydi.subscriptions.domain.ports.PlanRepositoryPort;
import com.affiliate.rentals.gydi.subscriptions.domain.ports.UserSubscriptionRepositoryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Use case for deleting a payment method (soft delete).
 *
 * <p>
 * This service implements soft delete functionality by marking payment methods
 * as INACTIVE
 * instead of physically removing them from the database. This approach
 * maintains audit trails
 * and allows for potential recovery while preventing the payment method from
 * being used.
 * </p>
 *
 * <p>
 * This service handles the payment method deletion flow with strict business
 * rules:
 * </p>
 * <ul>
 * <li>Users with paid subscriptions (PRO, ELITE) MUST keep at least one payment
 * method</li>
 * <li>Default payment method can only be deleted if it's the only one
 * remaining</li>
 * <li>Payment method is marked as INACTIVE (soft delete) instead of being
 * physically deleted</li>
 * <li>Stripe payment method is NOT detached to maintain payment gateway
 * consistency</li>
 * <li>Only the payment method owner can delete it</li>
 * </ul>
 *
 * <p>
 * <b>Security:</b> Ownership validation ensures users can only delete their own
 * payment methods.
 * </p>
 *
 * @author GYDI Development Team
 */
@Service
public class DeletePaymentMethodUseCase {

    private static final Logger logger = LoggerFactory.getLogger(DeletePaymentMethodUseCase.class);

    /**
     * Minimum number of payment methods required for users with paid subscriptions.
     * Users on paid plans (PRO, ELITE) must maintain at least one active payment
     * method.
     */
    private static final int MINIMUM_METHODS_FOR_PAID_SUBSCRIPTION = 1;

    private final PaymentMethodRepositoryPort paymentMethodRepository;
    private final UserSubscriptionRepositoryPort subscriptionRepository;
    private final PlanRepositoryPort planRepository;
    private final Optional<PaymentGatewayPort> paymentGateway;

    public DeletePaymentMethodUseCase(
            PaymentMethodRepositoryPort paymentMethodRepository,
            UserSubscriptionRepositoryPort subscriptionRepository,
            PlanRepositoryPort planRepository,
            Optional<PaymentGatewayPort> paymentGateway) {
        this.paymentMethodRepository = paymentMethodRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.planRepository = planRepository;
        this.paymentGateway = paymentGateway;
    }

    /**
     * Executes the delete payment method use case (soft delete).
     *
     * <p>
     * <b>Business Rules Enforced:</b>
     * </p>
     * <ol>
     * <li>Payment method must exist and belong to the user</li>
     * <li>Cannot delete the only payment method if user has a paid
     * subscription</li>
     * <li>Cannot delete default payment method if other payment methods exist</li>
     * <li>Payment method is marked as INACTIVE (soft delete) instead of physical
     * deletion</li>
     * <li>Stripe payment method is NOT detached to maintain gateway
     * consistency</li>
     * </ol>
     *
     * <p>
     * <b>Soft Delete Implementation:</b>
     * </p>
     * <p>
     * Instead of physically deleting the record, this method marks the payment
     * method as INACTIVE
     * by calling {@link PaymentMethod#deactivate()}. This approach:
     * </p>
     * <ul>
     * <li>Maintains audit trails for compliance and debugging</li>
     * <li>Allows for potential recovery if needed</li>
     * <li>Prevents the payment method from being used for future transactions</li>
     * <li>Keeps Stripe payment method attached for reference</li>
     * </ul>
     *
     * @param paymentMethodId the ID of the payment method to delete
     * @param userId          the ID of the authenticated user
     * @throws PaymentMethodNotFoundException     if payment method not found or
     *                                            doesn't belong to user
     * @throws CannotDeletePaymentMethodException if business rules prevent deletion
     */
    @Transactional
    public void execute(Long paymentMethodId, Long userId) {
        logger.info("Attempting to soft delete payment method {} for user {}", paymentMethodId, userId);

        PaymentMethod paymentMethod = findAndValidatePaymentMethod(paymentMethodId, userId);
        long activeMethodCount = paymentMethodRepository.countActiveByUserId(userId);

        validateDeletionBusinessRules(paymentMethod, userId, activeMethodCount);

        // Soft delete: mark as INACTIVE instead of physically deleting
        PaymentMethod inactiveMethod = paymentMethod.deactivate();
        paymentMethodRepository.save(inactiveMethod);

        logger.info("Successfully soft deleted payment method {} for user {} (status set to INACTIVE)",
                paymentMethodId, userId);
    }

    /**
     * Finds a payment method and validates that it belongs to the specified user.
     *
     * @param paymentMethodId the payment method ID
     * @param userId          the user ID
     * @return the payment method if found and owned by user
     * @throws PaymentMethodNotFoundException     if payment method not found
     * @throws CannotDeletePaymentMethodException if payment method doesn't belong
     *                                            to user
     */
    private PaymentMethod findAndValidatePaymentMethod(Long paymentMethodId, Long userId) {
        PaymentMethod paymentMethod = paymentMethodRepository.findById(paymentMethodId)
                .orElseThrow(() -> {
                    logger.warn("Payment method {} not found", paymentMethodId);
                    return PaymentMethodNotFoundException.withId(paymentMethodId);
                });

        if (!paymentMethod.userId().equals(userId)) {
            logger.warn("User {} attempted to delete payment method {} belonging to user {}",
                    userId, paymentMethodId, paymentMethod.userId());
            throw CannotDeletePaymentMethodException.notOwnedByUser();
        }

        return paymentMethod;
    }

    /**
     * Validates all business rules for payment method deletion.
     *
     * @param paymentMethod     the payment method to delete
     * @param userId            the user ID
     * @param activeMethodCount the number of active payment methods for this user
     * @throws CannotDeletePaymentMethodException if any business rule is violated
     */
    private void validateDeletionBusinessRules(PaymentMethod paymentMethod, Long userId, long activeMethodCount) {
        logger.debug("User {} has {} active payment method(s)", userId, activeMethodCount);

        validatePaidSubscriptionRequirement(userId, activeMethodCount);
        validateDefaultMethodDeletion(paymentMethod, userId, activeMethodCount);
    }

    /**
     * Validates that users with paid subscriptions maintain at least one payment
     * method.
     *
     * @param userId            the user ID
     * @param activeMethodCount the number of active payment methods
     * @throws CannotDeletePaymentMethodException if user has paid subscription and
     *                                            only one method
     */
    private void validatePaidSubscriptionRequirement(Long userId, long activeMethodCount) {
        Optional<UserSubscription> subscriptionOpt = subscriptionRepository.findByUserId(userId);

        if (subscriptionOpt.isEmpty()) {
            return; // No subscription, no restriction
        }

        UserSubscription subscription = subscriptionOpt.get();
        logger.debug("User {} has subscription with plan ID {}", userId, subscription.planId());

        Plan plan = planRepository.findById(subscription.planId())
                .orElseThrow(() -> PlanNotFoundException.withId(subscription.planId()));

        boolean hasOnlyOneMethod = activeMethodCount == MINIMUM_METHODS_FOR_PAID_SUBSCRIPTION;

        if (!plan.isFree() && hasOnlyOneMethod) {
            logger.warn("User {} attempted to delete the only payment method while on paid plan {}",
                    userId, plan.planCode());
            throw CannotDeletePaymentMethodException.onlyMethodOnPaidSubscription();
        }
    }

    /**
     * Validates that default payment methods can only be deleted when they are the
     * last method.
     *
     * @param paymentMethod     the payment method to delete
     * @param userId            the user ID
     * @param activeMethodCount the number of active payment methods
     * @throws CannotDeletePaymentMethodException if default method is being deleted
     *                                            while others exist
     */
    private void validateDefaultMethodDeletion(PaymentMethod paymentMethod, Long userId, long activeMethodCount) {
        boolean hasMultipleMethods = activeMethodCount > MINIMUM_METHODS_FOR_PAID_SUBSCRIPTION;

        if (paymentMethod.isDefault() && hasMultipleMethods) {
            logger.warn("User {} attempted to delete default payment method {} while other methods exist",
                    userId, paymentMethod.id());
            throw CannotDeletePaymentMethodException.isDefaultMethod();
        }
    }

    /*
     * NOTE: Stripe detachment methods removed for soft delete implementation.
     *
     * Rationale:
     * - Soft delete keeps the payment method in database for audit purposes
     * - Detaching from Stripe would break the reference and prevent recovery
     * - If hard delete is needed in the future, Stripe detachment should be
     * implemented
     * - For now, payment methods remain attached to Stripe Customer for consistency
     *
     * Original methods: detachFromStripe() and performStripeDetachment() - removed
     */
}
