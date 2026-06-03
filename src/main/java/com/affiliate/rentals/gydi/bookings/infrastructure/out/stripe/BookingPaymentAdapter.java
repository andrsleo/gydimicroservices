package com.affiliate.rentals.gydi.bookings.infrastructure.out.stripe;

import com.affiliate.rentals.gydi.bookings.domain.ports.BookingPaymentPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Stub adapter implementing BookingPaymentPort.
 * <p>
 * Phase 2 placeholder — full Stripe Connect integration is implemented in security task S4.
 * This adapter logs operations and returns placeholder IDs to allow the booking flow to compile.
 * </p>
 * <p>
 * TODO (S4): Replace stub with real Stripe SDK calls using idempotency keys.
 * </p>
 */
@Component
public class BookingPaymentAdapter implements BookingPaymentPort {

    private static final Logger log = LoggerFactory.getLogger(BookingPaymentAdapter.class);

    @Override
    public String createBookingPayment(Long bookingId, long amountCents, String currency,
                                       String guestPaymentMethodId) {
        log.warn("[STUB] createBookingPayment — bookingId={}, amount={} {}, pm={}",
                bookingId, amountCents, currency, guestPaymentMethodId);
        // TODO (S4): Implement Stripe PaymentIntent creation with capture_method=automatic
        return "pi_stub_booking_" + bookingId;
    }

    @Override
    public String createSecurityDepositHold(Long bookingId, long depositCents, String currency,
                                            String guestPaymentMethodId) {
        log.warn("[STUB] createSecurityDepositHold — bookingId={}, deposit={} {}, pm={}",
                bookingId, depositCents, currency, guestPaymentMethodId);
        // TODO (S4): Implement Stripe PaymentIntent creation with capture_method=manual
        return "pi_stub_deposit_" + bookingId;
    }

    @Override
    public void releasePaymentToHost(Long bookingId, String bookingPaymentIntentId,
                                     long hostAmountCents, String currency,
                                     String hostStripeAccountId) {
        log.warn("[STUB] releasePaymentToHost — bookingId={}, intentId={}, hostAmount={} {}, hostAccount={}",
                bookingId, bookingPaymentIntentId, hostAmountCents, currency, hostStripeAccountId);
        // TODO (S4): Implement Stripe Transfer to connected account
    }

    @Override
    public void releaseSecurityDeposit(Long bookingId, String depositPaymentIntentId) {
        log.warn("[STUB] releaseSecurityDeposit — bookingId={}, intentId={}",
                bookingId, depositPaymentIntentId);
        // TODO (S4): Implement Stripe PaymentIntent cancel
    }

    @Override
    public void captureSecurityDeposit(Long bookingId, String depositPaymentIntentId,
                                       Long amountToCaptureCents) {
        log.warn("[STUB] captureSecurityDeposit — bookingId={}, intentId={}, amount={}",
                bookingId, depositPaymentIntentId, amountToCaptureCents);
        // TODO (S4): Implement Stripe PaymentIntent capture with optional partial amount
    }
}
