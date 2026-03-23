package com.affiliate.rentals.gydi.subscriptions.application.usecase;

import com.affiliate.rentals.gydi.subscriptions.domain.exception.CannotDeletePaymentMethodException;
import com.affiliate.rentals.gydi.subscriptions.domain.exception.PaymentMethodNotFoundException;
import com.affiliate.rentals.gydi.subscriptions.domain.model.PaymentMethod;
import com.affiliate.rentals.gydi.subscriptions.domain.ports.PaymentGatewayPort;
import com.affiliate.rentals.gydi.subscriptions.domain.ports.PaymentMethodRepositoryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Use case for deleting a payment method (soft delete + Stripe detachment).
 *
 * <p>
 * Marks the payment method as INACTIVE in the database and detaches it from
 * Stripe so it can no longer be charged.
 * </p>
 *
 * <p>Business rules enforced:</p>
 * <ul>
 * <li>Users must keep at least one active payment method — deletion is blocked
 *     when only one remains, regardless of subscription plan.</li>
 * <li>The default payment method cannot be deleted while other methods exist —
 *     set another as default first.</li>
 * <li>Only the payment method owner can delete it.</li>
 * </ul>
 *
 * @author GYDI Development Team
 */
@Service
public class DeletePaymentMethodUseCase {

    private static final Logger logger = LoggerFactory.getLogger(DeletePaymentMethodUseCase.class);

    private final PaymentMethodRepositoryPort paymentMethodRepository;
    private final Optional<PaymentGatewayPort> paymentGateway;

    public DeletePaymentMethodUseCase(
            PaymentMethodRepositoryPort paymentMethodRepository,
            Optional<PaymentGatewayPort> paymentGateway) {
        this.paymentMethodRepository = paymentMethodRepository;
        this.paymentGateway = paymentGateway;
    }

    /**
     * Soft-deletes a payment method and detaches it from Stripe.
     *
     * @param paymentMethodId the ID of the payment method to delete
     * @param userId          the ID of the authenticated user
     * @throws PaymentMethodNotFoundException     if not found or not owned by user
     * @throws CannotDeletePaymentMethodException if business rules prevent deletion
     */
    @Transactional
    public void execute(Long paymentMethodId, Long userId) {
        logger.info("Attempting to delete payment method {} for user {}", paymentMethodId, userId);

        PaymentMethod paymentMethod = findAndValidatePaymentMethod(paymentMethodId, userId);
        long activeMethodCount = paymentMethodRepository.countActiveByUserId(userId);

        validateDeletionBusinessRules(paymentMethod, userId, activeMethodCount);

        PaymentMethod inactiveMethod = paymentMethod.deactivate();
        paymentMethodRepository.save(inactiveMethod);

        detachFromStripe(paymentMethod.gatewayToken());

        logger.info("Successfully deleted payment method {} for user {}", paymentMethodId, userId);
    }

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

    private void validateDeletionBusinessRules(PaymentMethod paymentMethod, Long userId, long activeMethodCount) {
        logger.debug("User {} has {} active payment method(s)", userId, activeMethodCount);

        if (activeMethodCount <= 1) {
            logger.warn("User {} attempted to delete their only active payment method", userId);
            throw CannotDeletePaymentMethodException.onlyActiveMethod();
        }

        if (paymentMethod.isDefault()) {
            logger.warn("User {} attempted to delete default payment method {} while other methods exist",
                    userId, paymentMethod.id());
            throw CannotDeletePaymentMethodException.isDefaultMethod();
        }
    }

    private void detachFromStripe(String gatewayToken) {
        paymentGateway.ifPresentOrElse(
                gw -> {
                    try {
                        gw.detachPaymentMethod(gatewayToken);
                        logger.info("Detached payment method {} from Stripe", gatewayToken);
                    } catch (Exception e) {
                        // Best-effort: soft delete already committed, log and continue
                        logger.warn("Failed to detach payment method {} from Stripe: {}", gatewayToken, e.getMessage());
                    }
                },
                () -> logger.warn("Payment gateway unavailable — skipping Stripe detachment for {}", gatewayToken)
        );
    }
}
