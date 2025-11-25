package com.affiliate.rentals.gydi.payment.domain.port.in;

import java.math.BigDecimal;

/**
 * Use Case: Create payment for finished booking.
 * <p>
 * Triggered automatically by BookingFinishedEvent.
 * Creates a payment.booking record with calculated commission.
 * </p>
 */
public interface CreatePaymentUseCase {

    /**
     * Creates a payment for a finished booking.
     */
    PaymentResponse execute(CreatePaymentCommand command);

    /**
     * Command to create payment.
     */
    record CreatePaymentCommand(
        Long bookingId,
        Long referralLinkId,
        Long userId,
        BigDecimal totalAmount,
        String currency,
        BigDecimal commissionPercentage
    ) {
    }

    /**
     * Response after creating payment.
     */
    record PaymentResponse(
        Long id,
        Long bookingId,
        Long userId,
        BigDecimal totalAmount,
        BigDecimal commissionAmount,
        BigDecimal commissionPercentage,
        String currency,
        String status,
        String createdAt
    ) {
    }
}
