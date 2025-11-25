package com.affiliate.rentals.gydi.payment.application.usecase;

import com.affiliate.rentals.gydi.payment.domain.event.PaymentCreatedEvent;
import com.affiliate.rentals.gydi.payment.domain.model.Money;
import com.affiliate.rentals.gydi.payment.domain.model.PaymentBooking;
import com.affiliate.rentals.gydi.payment.domain.port.in.CreatePaymentUseCase;
import com.affiliate.rentals.gydi.payment.domain.port.out.PaymentRepositoryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

/**
 * Use Case Implementation: Create payment for finished booking.
 * <p>
 * This is triggered automatically by BookingFinishedEventHandler.
 * Creates a payment record with calculated commission based on user's plan.
 * </p>
 */
@Service
@Transactional
public class CreatePaymentUseCaseImpl implements CreatePaymentUseCase {

    private static final Logger log = LoggerFactory.getLogger(CreatePaymentUseCaseImpl.class);
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final PaymentRepositoryPort paymentRepository;
    private final ApplicationEventPublisher eventPublisher;

    public CreatePaymentUseCaseImpl(PaymentRepositoryPort paymentRepository,
                                     ApplicationEventPublisher eventPublisher) {
        this.paymentRepository = paymentRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public PaymentResponse execute(CreatePaymentCommand command) {
        log.info("Creating payment for booking {}, user {}, commission {}%",
            command.bookingId(), command.userId(), command.commissionPercentage().multiply(BigDecimal.valueOf(100)));

        // 1. Check if payment already exists for this booking
        if (paymentRepository.existsByBookingId(command.bookingId())) {
            log.warn("Payment already exists for booking {}. Skipping creation.", command.bookingId());
            PaymentBooking existing = paymentRepository.findByBookingId(command.bookingId())
                .orElseThrow(); // Should exist based on existsByBookingId
            return mapToResponse(existing);
        }

        // 2. Create Money value object for totalAmount
        Money totalAmount = Money.of(command.totalAmount(), command.currency());

        // 3. Create PaymentBooking aggregate (commission calculated internally)
        PaymentBooking payment = PaymentBooking.createPending(
            command.bookingId(),
            command.referralLinkId(),
            command.userId(),
            totalAmount,
            command.commissionPercentage()
        );

        // 4. Persist payment
        PaymentBooking savedPayment = paymentRepository.save(payment);

        log.info("Payment created with ID: {}, commission: {}",
            savedPayment.getId(), savedPayment.getCommissionAmount());

        // 5. Publish domain event
        publishPaymentCreatedEvent(savedPayment);

        // 6. Map to response
        return mapToResponse(savedPayment);
    }

    /**
     * Publishes PaymentCreatedEvent for notifications/analytics.
     */
    private void publishPaymentCreatedEvent(PaymentBooking payment) {
        PaymentCreatedEvent event = new PaymentCreatedEvent(
            payment.getId(),
            payment.getBookingId(),
            payment.getUserId(),
            payment.getCommissionAmount().getAmount(),
            payment.getCommissionAmount().getCurrencyCode(),
            payment.getCreatedAt()
        );

        eventPublisher.publishEvent(event);
        log.debug("PaymentCreatedEvent published for payment {}", payment.getId());
    }

    private PaymentResponse mapToResponse(PaymentBooking payment) {
        return new PaymentResponse(
            payment.getId(),
            payment.getBookingId(),
            payment.getUserId(),
            payment.getTotalAmount().getAmount(),
            payment.getCommissionAmount().getAmount(),
            payment.getCommissionPercentage(),
            payment.getTotalAmount().getCurrencyCode(),
            payment.getStatus().name(),
            payment.getCreatedAt().format(DATE_TIME_FORMATTER)
        );
    }
}
