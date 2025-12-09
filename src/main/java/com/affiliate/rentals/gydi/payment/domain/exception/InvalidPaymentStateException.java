package com.affiliate.rentals.gydi.payment.domain.exception;

import com.affiliate.rentals.gydi.payment.domain.model.PaymentStatus;

/**
 * Exception thrown when attempting an invalid payment state transition.
 * <p>
 * Example: Trying to mark a SUCCESS payment as PROCESSING.
 * </p>
 */
public class InvalidPaymentStateException extends RuntimeException {

    private final PaymentStatus currentStatus;
    private final PaymentStatus targetStatus;

    public InvalidPaymentStateException(PaymentStatus currentStatus, PaymentStatus targetStatus) {
        super(String.format("Invalid payment state transition: %s → %s", currentStatus, targetStatus));
        this.currentStatus = currentStatus;
        this.targetStatus = targetStatus;
    }

    public PaymentStatus getCurrentStatus() {
        return currentStatus;
    }

    public PaymentStatus getTargetStatus() {
        return targetStatus;
    }
}
