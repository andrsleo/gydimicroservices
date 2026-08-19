package com.affiliate.rentals.gydi.subscriptions.infrastructure.in.rest.controller;

import com.affiliate.rentals.gydi.commissions.domain.ports.StripeConnectAccountRepositoryPort;
import com.affiliate.rentals.gydi.subscriptions.domain.model.Plan;
import com.affiliate.rentals.gydi.subscriptions.domain.ports.PaymentGatewayPort;
import com.affiliate.rentals.gydi.subscriptions.domain.ports.PlanRepositoryPort;
import com.affiliate.rentals.gydi.users.domain.model.Email;
import com.affiliate.rentals.gydi.users.domain.ports.UserRepositoryPort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Diagnostic endpoint to help troubleshoot Stripe integration issues.
 * 
 * <p>
 * <strong>SECURITY NOTE:</strong> This should only be enabled in
 * development/test environments.
 * Remove or protect this endpoint in production.
 * </p>
 */
@RestController
@RequestMapping("/api/admin/stripe-health")
public class StripeHealthController {

    private final Optional<PaymentGatewayPort> paymentGateway;
    private final PlanRepositoryPort planRepository;
    private final UserRepositoryPort userRepository;
    private final StripeConnectAccountRepositoryPort connectAccountRepository;

    public StripeHealthController(
            Optional<PaymentGatewayPort> paymentGateway,
            PlanRepositoryPort planRepository,
            UserRepositoryPort userRepository,
            StripeConnectAccountRepositoryPort connectAccountRepository) {
        this.paymentGateway = paymentGateway;
        this.planRepository = planRepository;
        this.userRepository = userRepository;
        this.connectAccountRepository = connectAccountRepository;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> checkStripeHealth(Authentication authentication) {
        Map<String, Object> health = new LinkedHashMap<>();

        // 1. Check if PaymentGateway is available
        health.put("timestamp", LocalDateTime.now().toString());
        health.put("paymentGatewayInjected", paymentGateway.isPresent());

        if (paymentGateway.isEmpty()) {
            health.put("error",
                    "PaymentGateway is not injected. Check @ConditionalOnProperty or Stripe API key configuration.");
            return ResponseEntity.ok(health);
        }

        // 2. Check plans configuration
        List<Plan> allPlans = planRepository.findAll();
        List<Map<String, Object>> planDetails = allPlans.stream().map(plan -> {
            Map<String, Object> details = new LinkedHashMap<>();
            details.put("planCode", plan.planCode());
            details.put("planName", plan.planName());
            details.put("monthlyPrice", plan.monthlyPrice());
            details.put("stripePriceId", plan.stripePriceId());
            details.put("hasStripePriceId", plan.stripePriceId() != null && !plan.stripePriceId().isBlank());
            details.put("isActive", plan.isActive());
            return details;
        }).collect(Collectors.toList());

        health.put("totalPlans", allPlans.size());
        health.put("plans", planDetails);

        long plansWithoutStripePriceId = allPlans.stream()
                .filter(p -> !p.isFree())
                .filter(p -> p.stripePriceId() == null || p.stripePriceId().isBlank())
                .count();

        if (plansWithoutStripePriceId > 0) {
            health.put("warning", plansWithoutStripePriceId + " paid plan(s) are missing stripePriceId");
        }

        // 3. Check current user's Stripe Connect account / platform customer ID
        if (authentication != null && authentication.isAuthenticated()) {
            Email email = new Email(authentication.getName());
            userRepository.findByEmail(email).ifPresent(user -> {
                String platformCustomerId = connectAccountRepository
                        .findPlatformCustomerIdByUserId(user.id()).orElse(null);
                Map<String, Object> userHealth = new LinkedHashMap<>();
                userHealth.put("userId", user.id());
                userHealth.put("email", user.email().address());
                userHealth.put("hasPlatformCustomerId", platformCustomerId != null);
                userHealth.put("platformCustomerId", platformCustomerId);
                health.put("currentUser", userHealth);

                if (platformCustomerId == null) {
                    health.put("userWarning",
                            "Current user does not have a platform customer ID. Lazy creation will trigger on first payment method add.");
                }
            });
        }

        // 4. Try to ping Stripe API
        try {
            // Simple test: try to create and immediately delete a test customer
            String testEmail = "health-check-" + System.currentTimeMillis() + "@test.gydi.com";
            PaymentGatewayPort.CustomerResult testCustomer = paymentGateway.get().createCustomer(
                    testEmail,
                    "Health Check Test",
                    "test:health-check");

            // Clean up
            paymentGateway.get().deleteCustomer(testCustomer.id());

            health.put("stripeApiConnectivity", "OK");
            health.put("stripeApiTest", "Successfully created and deleted test customer");

        } catch (Exception e) {
            health.put("stripeApiConnectivity", "FAILED");
            health.put("stripeApiError", e.getMessage());
            health.put("stripeApiSuggestion",
                    "Check API keys in application.properties and verify they are test keys (sk_test_...)");
        }

        return ResponseEntity.ok(health);
    }
}
