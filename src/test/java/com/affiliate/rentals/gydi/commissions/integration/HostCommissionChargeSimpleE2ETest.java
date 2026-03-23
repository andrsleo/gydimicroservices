package com.affiliate.rentals.gydi.commissions.integration;

import com.affiliate.rentals.gydi.commissions.application.usecase.ChargeHostCommissionUseCase;
import com.affiliate.rentals.gydi.commissions.domain.model.HostCommission;
import com.affiliate.rentals.gydi.commissions.domain.model.HostCommissionStatus;
import com.affiliate.rentals.gydi.commissions.domain.model.vo.CommissionAmount;
import com.affiliate.rentals.gydi.commissions.domain.ports.HostCommissionRepositoryPort;
import com.affiliate.rentals.gydi.commissions.domain.ports.PaymentGatewayPort;
import com.affiliate.rentals.gydi.subscriptions.domain.model.PaymentMethod;
import com.affiliate.rentals.gydi.subscriptions.domain.model.PaymentMethodType;
import com.affiliate.rentals.gydi.commissions.domain.model.ConnectAccountType;
import com.affiliate.rentals.gydi.commissions.domain.model.StripeConnectAccount;
import com.affiliate.rentals.gydi.commissions.domain.ports.StripeConnectAccountRepositoryPort;
import com.affiliate.rentals.gydi.subscriptions.domain.ports.PaymentMethodRepositoryPort;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Simplified E2E Integration Test: Host Commission Charge Flow
 * <p>
 * Host commission charges use a direct PaymentIntent on the platform account
 * (chargeHostCommission), NOT chargeHostViaConnect. This avoids requiring
 * card_payments capability on the Express Connect account (which only has
 * transfers capability for affiliate payouts).
 * </p>
 * Charge: platform charges host's cus_xxx for the commission amount only (not the full booking).
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Simple E2E Test: Host Commission Charge (Direct Platform Charge)")
class HostCommissionChargeSimpleE2ETest {

    @Autowired
    private ChargeHostCommissionUseCase chargeHostCommissionUseCase;

    @Autowired
    private HostCommissionRepositoryPort hostCommissionRepository;

    @MockBean
    private PaymentGatewayPort paymentGateway;

    @MockBean
    private StripeConnectAccountRepositoryPort connectAccountRepository;

    @MockBean
    private PaymentMethodRepositoryPort paymentMethodRepository;

    private HostCommission pendingCommission;
    private StripeConnectAccount mockConnectAccount;
    private PaymentMethod mockPaymentMethod;

    // Booking: $1000.00, commission rate: 20% → commission = $200.00
    private static final BigDecimal BOOKING_AMOUNT   = new BigDecimal("1000.00");
    private static final BigDecimal COMMISSION_RATE  = new BigDecimal("0.20");
    // Platform charges host ONLY the commission amount (not the full booking)
    private static final long COMMISSION_AMOUNT_CENTS = 20_000L; // $200.00

    @BeforeEach
    void setUp() {
        // Create mock Connect account — only needs stripePlatformCustomerId (cus_xxx)
        mockConnectAccount = StripeConnectAccount.create(100L, "acct_test_host", ConnectAccountType.STANDARD, "US");
        mockConnectAccount.setPlatformCustomerId("cus_test_host_123");

        // Create mock PaymentMethod
        mockPaymentMethod = PaymentMethod.builder()
            .id(1L)
            .userId(100L)
            .methodType(PaymentMethodType.CREDIT_CARD)
            .gatewayProvider("stripe")
            .gatewayToken("pm_test_card_visa")
            .cardLastFour("4242")
            .cardBrand("visa")
            .cardExpMonth(12)
            .cardExpYear(2030)
            .isDefault(true)
            .build();

        // Configure mocks
        when(connectAccountRepository.findByUserId(100L)).thenReturn(Optional.of(mockConnectAccount));
        when(paymentMethodRepository.findDefaultByUserId(100L)).thenReturn(Optional.of(mockPaymentMethod));

        // Create a PENDING host commission
        CommissionAmount amount = CommissionAmount.calculate(BOOKING_AMOUNT, COMMISSION_RATE, "USD");

        pendingCommission = HostCommission.create(
            1L,     // bookingId
            100L,   // hostId
            "PRO",  // hostPlan (20% commission rate)
            amount
        );

        pendingCommission = hostCommissionRepository.save(pendingCommission);
    }

    @AfterEach
    void tearDown() {
        if (pendingCommission != null && pendingCommission.getId() != null) {
            hostCommissionRepository.deleteById(pendingCommission.getId());
        }
    }

    @Test
    @DisplayName("Should charge commission directly and update status to CHARGED")
    void shouldChargeCommissionSuccessfully() {
        // GIVEN: Mock successful direct charge
        PaymentGatewayPort.PaymentResult successResult = new PaymentGatewayPort.PaymentResult(
            true,
            "pi_test_success_123",
            "ch_test_charge_456",
            null,
            LocalDateTime.now()
        );

        when(paymentGateway.chargeHostCommission(
            anyString(), // cus_xxx
            anyString(), // pm_xxx
            anyLong(),   // commissionAmountCents
            anyString(), // currency
            anyString(), // bookingId
            anyString()  // commissionId
        )).thenReturn(successResult);

        // WHEN: Execute charge
        chargeHostCommissionUseCase.execute(pendingCommission.getId());

        // THEN: Commission is CHARGED with correct Stripe IDs
        HostCommission charged = hostCommissionRepository
            .findById(pendingCommission.getId())
            .orElseThrow(() -> new AssertionError("Commission not found"));

        assertThat(charged.getStatus()).isEqualTo(HostCommissionStatus.CHARGED);
        assertThat(charged.getStripePaymentIntentId()).isEqualTo("pi_test_success_123");
        assertThat(charged.getStripeChargeId()).isEqualTo("ch_test_charge_456");
        assertThat(charged.getChargedAt()).isNotNull();

        // Verify Stripe was called with commission amount only (not full booking amount)
        verify(paymentGateway).chargeHostCommission(
            eq("cus_test_host_123"),    // host platform customer
            eq("pm_test_card_visa"),    // payment method token
            eq(COMMISSION_AMOUNT_CENTS), // 20000L ($200 commission only)
            eq("USD"),
            eq("1"),                    // bookingId
            eq(pendingCommission.getId().toString())
        );
    }

    @Test
    @DisplayName("Should handle payment failure and mark commission as FAILED")
    void shouldHandlePaymentFailure() {
        // GIVEN: Mock Stripe payment failure
        PaymentGatewayPort.PaymentResult failureResult = new PaymentGatewayPort.PaymentResult(
            false,
            null,
            null,
            "Your card has insufficient funds.",
            LocalDateTime.now()
        );

        when(paymentGateway.chargeHostCommission(
            anyString(), anyString(), anyLong(), anyString(), anyString(), anyString()
        )).thenReturn(failureResult);

        // WHEN: Execute charge
        chargeHostCommissionUseCase.execute(pendingCommission.getId());

        // THEN: Commission is FAILED with the Stripe reason
        HostCommission failed = hostCommissionRepository
            .findById(pendingCommission.getId())
            .orElseThrow(() -> new AssertionError("Commission not found"));

        assertThat(failed.getStatus()).isEqualTo(HostCommissionStatus.FAILED);
        assertThat(failed.getFailureReason()).contains("insufficient funds");
        assertThat(failed.getAttemptCount()).isEqualTo(1);
        assertThat(failed.getStripePaymentIntentId()).isNull();
    }

    @Test
    @DisplayName("Should charge only the commission amount in cents (not the full booking amount)")
    void shouldCalculateCorrectAmountInCents() {
        // GIVEN: Commission amount = $200 (20% of $1000 booking)
        assertThat(pendingCommission.getAmount().getCommissionAmount())
            .isEqualByComparingTo(new BigDecimal("200.00"));

        // GIVEN: Mock successful payment
        PaymentGatewayPort.PaymentResult successResult = new PaymentGatewayPort.PaymentResult(
            true, "pi_test", "ch_test", null, LocalDateTime.now()
        );

        when(paymentGateway.chargeHostCommission(
            anyString(), anyString(), anyLong(), anyString(), anyString(), anyString()
        )).thenReturn(successResult);

        // WHEN: Execute charge
        chargeHostCommissionUseCase.execute(pendingCommission.getId());

        // THEN: Stripe was called with commission amount = 20000L ($200.00), NOT 100000L ($1000)
        verify(paymentGateway).chargeHostCommission(
            anyString(),
            anyString(),
            eq(COMMISSION_AMOUNT_CENTS), // 20000L = $200.00 commission only
            anyString(),
            anyString(),
            anyString()
        );
    }

    @Test
    @DisplayName("Should update commission atomically on charge success")
    void shouldUpdateCommissionAtomically() {
        // GIVEN: Initial PENDING state
        assertThat(pendingCommission.getStatus()).isEqualTo(HostCommissionStatus.PENDING);
        assertThat(pendingCommission.getStripePaymentIntentId()).isNull();

        // GIVEN: Mock successful payment
        PaymentGatewayPort.PaymentResult successResult = new PaymentGatewayPort.PaymentResult(
            true, "pi_atomic_test", "ch_atomic", null, LocalDateTime.now()
        );

        when(paymentGateway.chargeHostCommission(
            anyString(), anyString(), anyLong(), anyString(), anyString(), anyString()
        )).thenReturn(successResult);

        // WHEN: Execute charge
        chargeHostCommissionUseCase.execute(pendingCommission.getId());

        // THEN: All fields updated atomically
        HostCommission updated = hostCommissionRepository
            .findById(pendingCommission.getId())
            .orElseThrow();

        assertThat(updated.getStatus()).isEqualTo(HostCommissionStatus.CHARGED);
        assertThat(updated.getStripePaymentIntentId()).isNotNull();
        assertThat(updated.getChargedAt()).isNotNull();
        assertThat(updated.getAttemptCount()).isEqualTo(0);
        assertThat(updated.getFailureReason()).isNull();
    }
}
