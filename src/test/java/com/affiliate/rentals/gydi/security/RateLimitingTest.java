package com.affiliate.rentals.gydi.security;

import com.affiliate.rentals.gydi.shared.security.RateLimitService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * Unit tests for Rate Limiting functionality.
 *
 * <p>These tests verify that the rate limiting mechanism correctly blocks
 * excessive authentication attempts to prevent brute force attacks.</p>
 *
 * <p><b>SECURITY: Brute Force Prevention Tests</b></p>
 *
 * @author GYDI Development Team
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Rate Limiting Tests")
class RateLimitingTest {

    @Mock
    private HttpServletRequest request;

    private RateLimitService rateLimitService;

    private static final String TEST_IP = "192.168.1.100";

    @BeforeEach
    void setUp() {
        rateLimitService = new RateLimitService();

        // Mock IP address (lenient for tests that override)
        lenient().when(request.getRemoteAddr()).thenReturn(TEST_IP);
        lenient().when(request.getHeader("X-Forwarded-For")).thenReturn(null);
    }

    @Test
    @DisplayName("Should allow first 5 authentication attempts")
    void shouldAllowFirst5Attempts() {
        // Given: Fresh rate limit bucket

        // When: Make 5 attempts
        for (int i = 1; i <= 5; i++) {
            boolean allowed = rateLimitService.tryConsumeAuth(request);

            // Then: All attempts should be allowed
            assertThat(allowed)
                    .as("Attempt %d should be allowed", i)
                    .isTrue();
        }
    }

    @Test
    @DisplayName("Should block 6th authentication attempt (brute force)")
    void shouldBlock6thAttempt() {
        // Given: 5 attempts already made
        for (int i = 1; i <= 5; i++) {
            rateLimitService.tryConsumeAuth(request);
        }

        // When: Make 6th attempt
        boolean allowed = rateLimitService.tryConsumeAuth(request);

        // Then: Should be blocked
        assertThat(allowed)
                .as("6th attempt should be blocked")
                .isFalse();
    }

    @Test
    @DisplayName("Should track remaining attempts correctly")
    void shouldTrackRemainingAttempts() {
        // Given: Fresh rate limit bucket

        // When: Make 3 attempts
        rateLimitService.tryConsumeAuth(request);
        rateLimitService.tryConsumeAuth(request);
        rateLimitService.tryConsumeAuth(request);

        long remaining = rateLimitService.getRemainingAuthAttempts(request);

        // Then: Should have 2 attempts remaining (5 - 3 = 2)
        assertThat(remaining).isEqualTo(2);
    }

    @Test
    @DisplayName("Should show 0 remaining attempts after hitting limit")
    void shouldShow0RemainingAfterHittingLimit() {
        // Given: 5 attempts already made
        for (int i = 1; i <= 5; i++) {
            rateLimitService.tryConsumeAuth(request);
        }

        // When: Check remaining attempts
        long remaining = rateLimitService.getRemainingAuthAttempts(request);

        // Then: Should be 0
        assertThat(remaining).isEqualTo(0);
    }

    @Test
    @DisplayName("Should isolate rate limits by IP address")
    void shouldIsolateByIpAddress() {
        // Given: IP1 has exhausted attempts
        when(request.getRemoteAddr()).thenReturn("192.168.1.100");
        for (int i = 1; i <= 5; i++) {
            rateLimitService.tryConsumeAuth(request);
        }
        boolean ip1Blocked = rateLimitService.tryConsumeAuth(request);

        // When: Different IP tries to authenticate
        when(request.getRemoteAddr()).thenReturn("192.168.1.200");
        boolean ip2Allowed = rateLimitService.tryConsumeAuth(request);

        // Then: IP1 should be blocked, IP2 should be allowed
        assertThat(ip1Blocked).isFalse();
        assertThat(ip2Allowed).isTrue();
    }

    @Test
    @DisplayName("Should use X-Forwarded-For header when present (proxy scenario)")
    void shouldUseXForwardedForHeader() {
        // Given: Request comes through proxy
        when(request.getHeader("X-Forwarded-For")).thenReturn("203.0.113.1, 198.51.100.1");

        // When: Make 6 attempts through proxy
        for (int i = 1; i <= 6; i++) {
            boolean allowed = rateLimitService.tryConsumeAuth(request);

            if (i <= 5) {
                // First 5 attempts should be allowed
                assertThat(allowed)
                        .as("Attempt %d should be allowed", i)
                        .isTrue();
            } else {
                // 6th attempt should be blocked
                assertThat(allowed)
                        .as("Attempt %d should be blocked", i)
                        .isFalse();
            }
        }
    }

    /**
     * Brute force attack simulation
     */
    @Test
    @DisplayName("Brute Force Scenario: 10 rapid login attempts")
    void bruteForceScenario_10RapidAttempts() {
        // Given: Attacker IP
        when(request.getRemoteAddr()).thenReturn("10.0.0.1");

        // When: Attacker makes 10 rapid login attempts
        int successfulAttempts = 0;
        int blockedAttempts = 0;

        for (int i = 1; i <= 10; i++) {
            boolean allowed = rateLimitService.tryConsumeAuth(request);
            if (allowed) {
                successfulAttempts++;
            } else {
                blockedAttempts++;
            }
        }

        // Then: Only first 5 attempts should succeed
        assertThat(successfulAttempts).isEqualTo(5);
        assertThat(blockedAttempts).isEqualTo(5);
    }

    /**
     * Password spray attack simulation (same IP, different usernames)
     */
    @Test
    @DisplayName("Password Spray Scenario: Same IP attacking multiple accounts")
    void passwordSprayScenario() {
        // Given: Attacker IP trying different accounts
        when(request.getRemoteAddr()).thenReturn("10.0.0.2");

        // When: Attacker tries 10 different accounts from same IP
        int successfulAttempts = 0;
        int blockedAttempts = 0;

        for (int i = 1; i <= 10; i++) {
            // Each attempt is for a different username, but same IP
            boolean allowed = rateLimitService.tryConsumeAuth(request);
            if (allowed) {
                successfulAttempts++;
            } else {
                blockedAttempts++;
            }
        }

        // Then: Rate limit should apply regardless of username (IP-based)
        assertThat(successfulAttempts).isEqualTo(5);
        assertThat(blockedAttempts).isEqualTo(5);
    }

    /**
     * Distributed attack from multiple IPs
     */
    @Test
    @DisplayName("Distributed Attack Scenario: Multiple IPs attacking same account")
    void distributedAttackScenario() {
        // Given: Multiple attacker IPs

        // When: 3 different IPs each make 5 attempts
        for (int ip = 1; ip <= 3; ip++) {
            when(request.getRemoteAddr()).thenReturn("10.0.0." + ip);

            for (int attempt = 1; attempt <= 5; attempt++) {
                boolean allowed = rateLimitService.tryConsumeAuth(request);

                // Then: Each IP gets 5 attempts
                assertThat(allowed)
                        .as("IP 10.0.0.%d attempt %d should be allowed", ip, attempt)
                        .isTrue();
            }

            // And: 6th attempt from each IP should be blocked
            boolean blocked = rateLimitService.tryConsumeAuth(request);
            assertThat(blocked)
                    .as("IP 10.0.0.%d 6th attempt should be blocked", ip)
                    .isFalse();
        }
    }
}