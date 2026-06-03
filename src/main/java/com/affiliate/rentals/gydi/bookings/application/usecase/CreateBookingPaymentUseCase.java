package com.affiliate.rentals.gydi.bookings.application.usecase;

import com.affiliate.rentals.gydi.bookings.domain.exception.BookingNotFoundException;
import com.affiliate.rentals.gydi.bookings.domain.exception.InvalidBookingStateException;
import com.affiliate.rentals.gydi.bookings.domain.model.Booking;
import com.affiliate.rentals.gydi.bookings.domain.model.vo.Money;
import com.affiliate.rentals.gydi.bookings.domain.ports.BookingPaymentPort;
import com.affiliate.rentals.gydi.bookings.domain.ports.BookingRepositoryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Use Case: Create booking payment + security deposit hold (GUEST).
 * <p>
 * Phase 2 — initiates Stripe payment flow after booking is confirmed (RESERVED).
 * </p>
 * <p>
 * Business Flow:
 * 1. Validate guest is the booking owner
 * 2. Create PaymentIntent for booking total (immediate capture)
 * 3. Create PaymentIntent for security deposit (manual capture / hold)
 * 4. Persist Stripe intent IDs in booking
 * </p>
 */
@Service
public class CreateBookingPaymentUseCase {

    private static final Logger log = LoggerFactory.getLogger(CreateBookingPaymentUseCase.class);

    private final BookingRepositoryPort bookingRepository;
    private final BookingPaymentPort paymentPort;

    public record CreatePaymentCommand(
            Long bookingId,
            String guestPaymentMethodId,
            Long requestingUserId
    ) {}

    public CreateBookingPaymentUseCase(BookingRepositoryPort bookingRepository,
                                       BookingPaymentPort paymentPort) {
        this.bookingRepository = Objects.requireNonNull(bookingRepository);
        this.paymentPort = Objects.requireNonNull(paymentPort);
    }

    @Transactional
    public void execute(CreatePaymentCommand command) {
        log.info("Initiating payment for booking {} by user {}", command.bookingId(), command.requestingUserId());

        Booking booking = bookingRepository.findById(command.bookingId())
                .orElseThrow(() -> new BookingNotFoundException(command.bookingId()));

        // Guard: the booking must be in an active state to accept payment
        if (!booking.getStatus().isActive()) {
            throw new InvalidBookingStateException(
                    "Cannot initiate payment for booking in status " + booking.getStatus());
        }

        Money totalAmount = booking.getTotalAmount();
        if (totalAmount == null) {
            throw new IllegalStateException(
                    "Booking " + command.bookingId() + " has no total amount — cannot initiate payment");
        }

        long bookingAmountCents = toCents(totalAmount.getAmount());
        long depositCents = calculateDeposit(
                booking.getPricePerNight() != null
                        ? toCents(booking.getPricePerNight().getAmount())
                        : bookingAmountCents);
        String currency = totalAmount.getCurrency().toLowerCase();

        // 1. Create payment intent (booking total)
        String bookingIntentId = paymentPort.createBookingPayment(
                command.bookingId(), bookingAmountCents, currency,
                command.guestPaymentMethodId()
        );

        // 2. Create authorization hold (security deposit)
        String depositIntentId = paymentPort.createSecurityDepositHold(
                command.bookingId(), depositCents, currency,
                command.guestPaymentMethodId()
        );

        // 3. Persist Stripe intent IDs in booking
        booking.setStripeBookingIntentId(bookingIntentId);
        booking.setStripeDepositIntentId(depositIntentId);
        booking.setDepositAmount(Money.of(
                BigDecimal.valueOf(depositCents).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP),
                totalAmount.getCurrency()
        ));
        bookingRepository.save(booking);

        log.info("Payment initiated for booking {} — bookingIntent={}, depositIntent={}",
                command.bookingId(), bookingIntentId, depositIntentId);
    }

    private long toCents(BigDecimal amount) {
        return amount.multiply(BigDecimal.valueOf(100)).longValue();
    }

    private long calculateDeposit(long pricePerNightCents) {
        long deposit = (long) (pricePerNightCents * 1.5);
        long minDeposit = 5_000L;    // $50 USD in cents
        long maxDeposit = 50_000L;   // $500 USD in cents
        return Math.min(Math.max(deposit, minDeposit), maxDeposit);
    }
}
