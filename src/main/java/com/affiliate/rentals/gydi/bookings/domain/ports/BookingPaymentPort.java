package com.affiliate.rentals.gydi.bookings.domain.ports;

/**
 * Output port for booking payment operations.
 * <p>
 * Abstracts Stripe payment operations for bookings and security deposits.
 * Implementation: {@link com.affiliate.rentals.gydi.bookings.infrastructure.out.stripe.BookingPaymentAdapter}
 * </p>
 * <p>
 * NOTE: Fase 2 — stub implementation. Full Stripe Connect integration in S4.
 * </p>
 */
public interface BookingPaymentPort {

    /**
     * Creates a PaymentIntent with immediate capture (booking payment).
     * Funds are held until transferred to the host.
     *
     * @return Stripe PaymentIntent ID
     */
    String createBookingPayment(Long bookingId, long amountCents, String currency,
                                 String guestPaymentMethodId);

    /**
     * Creates a PaymentIntent with MANUAL capture (security deposit hold).
     * The guest is NOT charged — only the amount is blocked on their card.
     *
     * @return Stripe PaymentIntent ID
     */
    String createSecurityDepositHold(Long bookingId, long depositCents, String currency,
                                      String guestPaymentMethodId);

    /**
     * Transfers the booking payment to the host (triggered at check-in).
     */
    void releasePaymentToHost(Long bookingId, String bookingPaymentIntentId,
                               long hostAmountCents, String currency,
                               String hostStripeAccountId);

    /**
     * Cancels the authorization hold on the security deposit (clean check-out).
     */
    void releaseSecurityDeposit(Long bookingId, String depositPaymentIntentId);

    /**
     * Captures partial or full security deposit (damage reported).
     *
     * @param amountToCaptureCents if null, captures the full authorized amount
     */
    void captureSecurityDeposit(Long bookingId, String depositPaymentIntentId,
                                 Long amountToCaptureCents);
}
