package com.affiliate.rentals.gydi.payment.domain.exception;

/**
 * Exception thrown when a payment cannot be found by ID.
 */
public class PaymentNotFoundException extends RuntimeException {

    private final Long paymentId;

    public PaymentNotFoundException(Long paymentId) {
        super("Payment not found with ID: " + paymentId);
        this.paymentId = paymentId;
    }

    public Long getPaymentId() {
        return paymentId;
    }
}
