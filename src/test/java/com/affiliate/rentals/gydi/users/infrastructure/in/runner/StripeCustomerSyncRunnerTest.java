package com.affiliate.rentals.gydi.users.infrastructure.in.runner;

import com.affiliate.rentals.gydi.users.domain.ports.UserStripePort;
import com.affiliate.rentals.gydi.users.domain.ports.UserStripePort.UserStripeInfo;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.param.CustomerCreateParams;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.ApplicationArguments;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link StripeCustomerSyncRunner}.
 *
 * <p>The Stripe SDK uses static methods ({@link Customer#create}), so this
 * test class relies on Mockito's static mocking support to intercept those
 * calls without making real HTTP requests.</p>
 *
 * <p>The {@link Stripe#apiKey} static field is set / cleared around each test
 * so that the guard logic inside the runner behaves identically to production.</p>
 *
 * @author GYDI Development Team
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("StripeCustomerSyncRunner unit tests")
class StripeCustomerSyncRunnerTest {

    @Mock
    private UserStripePort userStripePort;

    @Mock
    private ApplicationArguments applicationArguments;

    private StripeCustomerSyncRunner runner;

    /** Saved so we can restore the original value after each test. */
    private String originalStripeApiKey;

    @BeforeEach
    void setUp() {
        runner = new StripeCustomerSyncRunner(userStripePort);
        originalStripeApiKey = Stripe.apiKey;
    }

    @AfterEach
    void tearDown() {
        Stripe.apiKey = originalStripeApiKey;
    }

    // ── Guard: no API key configured ─────────────────────────────────────────

    @Test
    @DisplayName("Should skip sync when Stripe API key is null")
    void shouldSkipSync_whenStripeApiKeyIsNull() throws Exception {
        Stripe.apiKey = null;

        runner.run(applicationArguments);

        verify(userStripePort, never()).findActiveUsersWithoutPlatformCustomer();
        verify(userStripePort, never()).updatePlatformCustomerId(any(), any());
    }

    @Test
    @DisplayName("Should skip sync when Stripe API key is blank")
    void shouldSkipSync_whenStripeApiKeyIsBlank() throws Exception {
        Stripe.apiKey = "   ";

        runner.run(applicationArguments);

        verify(userStripePort, never()).findActiveUsersWithoutPlatformCustomer();
        verify(userStripePort, never()).updatePlatformCustomerId(any(), any());
    }

    // ── Guard: no pending users ───────────────────────────────────────────────

    @Test
    @DisplayName("Should skip Stripe API calls when no users are pending")
    void shouldSkipStripeApiCalls_whenNoPendingUsers() throws Exception {
        Stripe.apiKey = "sk_test_dummy_key_for_unit_tests";
        when(userStripePort.findActiveUsersWithoutPlatformCustomer())
                .thenReturn(Collections.emptyList());

        try (MockedStatic<Customer> customerStatic = Mockito.mockStatic(Customer.class)) {
            runner.run(applicationArguments);

            customerStatic.verify(() -> Customer.create(any(CustomerCreateParams.class)), never());
        }

        verify(userStripePort, never()).updatePlatformCustomerId(any(), any());
    }

    // ── Happy path ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Should create Stripe customer and update DB for each pending user")
    void shouldCreateStripeCustomerAndUpdateDb_forEachPendingUser() throws Exception {
        Stripe.apiKey = "sk_test_dummy_key_for_unit_tests";

        List<UserStripeInfo> pendingUsers = List.of(
                new UserStripeInfo(1L, "alice@example.com", "US", "FREE"),
                new UserStripeInfo(2L, "bob@example.com", null, "PRO")
        );
        when(userStripePort.findActiveUsersWithoutPlatformCustomer()).thenReturn(pendingUsers);

        Customer mockCustomerAlice = mock(Customer.class);
        when(mockCustomerAlice.getId()).thenReturn("cus_alice123");

        Customer mockCustomerBob = mock(Customer.class);
        when(mockCustomerBob.getId()).thenReturn("cus_bob456");

        try (MockedStatic<Customer> customerStatic = Mockito.mockStatic(Customer.class)) {
            customerStatic.when(() -> Customer.create(any(CustomerCreateParams.class)))
                    .thenReturn(mockCustomerAlice)
                    .thenReturn(mockCustomerBob);

            runner.run(applicationArguments);

            customerStatic.verify(() -> Customer.create(any(CustomerCreateParams.class)), times(2));
        }

        verify(userStripePort).updatePlatformCustomerId(1L, "cus_alice123");
        verify(userStripePort).updatePlatformCustomerId(2L, "cus_bob456");
    }

    // ── Fault tolerance ───────────────────────────────────────────────────────

    @Test
    @DisplayName("Should continue processing remaining users when Stripe throws for one user")
    void shouldContinueProcessing_whenStripeFails_forOneUser() throws Exception {
        Stripe.apiKey = "sk_test_dummy_key_for_unit_tests";

        List<UserStripeInfo> pendingUsers = List.of(
                new UserStripeInfo(10L, "fail@example.com", "MX", "FREE"),
                new UserStripeInfo(11L, "ok@example.com", "CO", "FREE")
        );
        when(userStripePort.findActiveUsersWithoutPlatformCustomer()).thenReturn(pendingUsers);

        StripeException stripeException = mock(StripeException.class);
        when(stripeException.getMessage()).thenReturn("card_error");

        Customer mockCustomerOk = mock(Customer.class);
        when(mockCustomerOk.getId()).thenReturn("cus_ok789");

        try (MockedStatic<Customer> customerStatic = Mockito.mockStatic(Customer.class)) {
            customerStatic.when(() -> Customer.create(any(CustomerCreateParams.class)))
                    .thenThrow(stripeException)
                    .thenReturn(mockCustomerOk);

            runner.run(applicationArguments);

            customerStatic.verify(() -> Customer.create(any(CustomerCreateParams.class)), times(2));
        }

        // First user failed — no update should have been called for them
        verify(userStripePort, never()).updatePlatformCustomerId(org.mockito.ArgumentMatchers.eq(10L), any());

        // Second user succeeded
        verify(userStripePort).updatePlatformCustomerId(11L, "cus_ok789");
    }
}
