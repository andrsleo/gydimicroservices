package com.affiliate.rentals.gydi.payment.domain.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Domain Event: Payment record created.
 * <p>
 * Published when a payment is created for a finished booking.
 * Can be used for notifications, analytics, or triggering external payment processing.
 * </p>
 */
public record PaymentCreatedEvent(
    Long paymentId,
    Long bookingId,
    Long userId,
    BigDecimal commissionAmount,
    String currency,
    LocalDateTime createdAt
) {
}
