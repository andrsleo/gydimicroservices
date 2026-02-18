package com.affiliate.rentals.gydi.commissions.integration;

import com.affiliate.rentals.gydi.commissions.application.usecase.ChargeHostCommissionUseCase;
import com.affiliate.rentals.gydi.commissions.domain.model.HostCommission;
import com.affiliate.rentals.gydi.commissions.domain.model.HostCommissionStatus;
import com.affiliate.rentals.gydi.commissions.domain.model.vo.CommissionAmount;
import com.affiliate.rentals.gydi.commissions.domain.ports.HostCommissionRepositoryPort;
import com.affiliate.rentals.gydi.commissions.domain.ports.PaymentGatewayPort;
import com.affiliate.rentals.gydi.subscriptions.domain.model.PaymentMethod;
import com.affiliate.rentals.gydi.subscriptions.domain.model.PaymentMethodType;
import com.affiliate.rentals.gydi.subscriptions.domain.ports.PaymentMethodRepositoryPort;
import com.affiliate.rentals.gydi.users.domain.model.Email;
import com.affiliate.rentals.gydi.users.domain.model.User;
import com.affiliate.rentals.gydi.users.domain.ports.UserRepositoryPort;
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
 * This test focuses ONLY on the commission charging logic,
 * without dependencies on bookings, properties, or users bounded contexts.
 * </p>
 * <p>
 * Test Scenario:
 * 1. Create a PENDING host commission manually
 * 2. Mock successful Stripe payment
 * 3. Execute ChargeHostCommissionUseCase
 * 4. Verify commission status changed to CHARGED
 * 5. Verify Stripe Payment Intent ID was saved
 * </p>
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Simple E2E Test: Host Commission Charge")
class HostCommissionChargeSimpleE2ETest {

    @Autowired
    private ChargeHostCommissionUseCase chargeHostCommissionUseCase;

    @Autowired
    private HostCommissionRepositoryPort hostCommissionRepository;

    @MockBean
    private PaymentGatewayPort paymentGateway;

    @MockBean
    private UserRepositoryPort userRepository;

    @MockBean
    private PaymentMethodRepositoryPort paymentMethodRepository;

    private HostCommission pendingCommission;
    private User mockHost;
    private PaymentMethod mockPaymentMethod;

    @BeforeEach
    void setUp() {
        // Create mock Host user
        mockHost = User.builder()
            .id(100L)
            .name("Test Host")
            .email(Email.of("host@test.com"))
            .passwordHash("hashed_password_test")
            .stripeCustomerId("cus_test_host_123")
            .build();

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
        when(userRepository.findById(100L)).thenReturn(Optional.of(mockHost));
        when(paymentMethodRepository.findDefaultByUserId(100L))
            .thenReturn(Optional.of(mockPaymentMethod));

        // Create a PENDING host commission with test data
        CommissionAmount amount = CommissionAmount.calculate(
            new BigDecimal("1000.00"),  // Booking amount
            new BigDecimal("0.20"),      // 20% commission rate (PRO plan)
            "USD"
        );

        pendingCommission = HostCommission.create(
            1L,         // bookingId
            100L,       // hostId
            "PRO",      // hostPlan (20% commission rate)
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
    @DisplayName("Should charge commission successfully and update status to CHARGED")
    void shouldChargeCommissionSuccessfully() {
        // GIVEN: Mock successful Stripe payment
        PaymentGatewayPort.PaymentResult successResult = new PaymentGatewayPort.PaymentResult(
            true,
            "pi_test_success_123",
            "ch_test_charge_456",
            null, // No failure reason on success
            LocalDateTime.now()
        );

        when(paymentGateway.chargeHostCommission(
            anyString(), // customer ID
            anyString(), // payment method token
            anyLong(),   // amount in cents
            anyString(), // currency
            anyString(), // booking ID
            anyString()  // commission ID
        )).thenReturn(successResult);

        // WHEN: Execute charge
        chargeHostCommissionUseCase.execute(pendingCommission.getId());

        // THEN: Verify commission was charged
        HostCommission chargedCommission = hostCommissionRepository
            .findById(pendingCommission.getId())
            .orElseThrow(() -> new AssertionError("Commission not found"));

        assertThat(chargedCommission.getStatus()).isEqualTo(HostCommissionStatus.CHARGED);
        assertThat(chargedCommission.getStripePaymentIntentId()).isEqualTo("pi_test_success_123");
        assertThat(chargedCommission.getStripeChargeId()).isEqualTo("ch_test_charge_456");
        assertThat(chargedCommission.getChargedAt()).isNotNull();

        // Verify Stripe was called with correct amount (20% of $1000 = $200 = 20000 cents)
        verify(paymentGateway).chargeHostCommission(
            anyString(),
            anyString(),
            eq(20000L), // $200.00 in cents
            eq("USD"),
            eq("1"),    // booking ID
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

        // THEN: Verify commission was marked as FAILED
        HostCommission failedCommission = hostCommissionRepository
            .findById(pendingCommission.getId())
            .orElseThrow(() -> new AssertionError("Commission not found"));

        assertThat(failedCommission.getStatus()).isEqualTo(HostCommissionStatus.FAILED);
        assertThat(failedCommission.getFailureReason()).contains("insufficient funds");
        assertThat(failedCommission.getAttemptCount()).isEqualTo(1);
        assertThat(failedCommission.getStripePaymentIntentId()).isNull();
    }

    @Test
    @DisplayName("Should calculate correct amount in cents for Stripe")
    void shouldCalculateCorrectAmountInCents() {
        // GIVEN: Commission with $200 amount
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

        // THEN: Verify Stripe was called with 20000 cents ($200.00)
        verify(paymentGateway).chargeHostCommission(
            anyString(),
            anyString(),
            eq(20000L), // $200.00 = 20000 cents
            anyString(),
            anyString(),
            anyString()
        );
    }

    @Test
    @DisplayName("Should update commission atomically")
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

        // THEN: Verify atomic update (all fields updated together)
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
