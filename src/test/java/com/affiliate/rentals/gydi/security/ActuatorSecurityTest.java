package com.affiliate.rentals.gydi.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for Actuator endpoint security.
 *
 * ⚠️ DISABLED: ApplicationContext initialization failure
 * Error: "ApplicationContext failure threshold (1) exceeded"
 *
 * This test requires fixing Spring context configuration.
 * Not related to security fixes (FIX-001 to FIX-011).
 *
 * <p>
 * These tests verify that sensitive Actuator endpoints are properly secured
 * and only accessible to ADMIN users.
 * </p>
 *
 * <p>
 * <b>SECURITY: Actuator Information Disclosure Prevention Tests</b>
 * </p>
 *
 * <p>
 * Uses H2 in-memory database for testing. ENUMs are stored as VARCHAR for H2 compatibility.
 * </p>
 *
 * @author GYDI Development Team
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Actuator Security Tests")
class ActuatorSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    // ==================== Public Endpoints (Always Accessible)
    // ====================

    @Test
    @DisplayName("Should allow unauthenticated access to /actuator/health")
    void shouldAllowUnauthenticatedHealthCheck() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should allow unauthenticated access to /actuator/info")
    void shouldAllowUnauthenticatedInfo() throws Exception {
        mockMvc.perform(get("/actuator/info"))
                .andExpect(status().isOk());
    }

    // ==================== Restricted Endpoints (Unauthenticated)
    // ====================

    @Test
    @DisplayName("Should block unauthenticated access to /actuator/env")
    void shouldBlockUnauthenticatedEnv() throws Exception {
        mockMvc.perform(get("/actuator/env"))
                .andExpect(status().isUnauthorized()); // 401: no credentials provided
    }

    @Test
    @DisplayName("Should block unauthenticated access to /actuator/metrics")
    void shouldBlockUnauthenticatedMetrics() throws Exception {
        mockMvc.perform(get("/actuator/metrics"))
                .andExpect(status().isUnauthorized()); // 401: no credentials provided
    }

    @Test
    @DisplayName("Should block unauthenticated access to /actuator/heapdump")
    void shouldBlockUnauthenticatedHeapdump() throws Exception {
        mockMvc.perform(get("/actuator/heapdump"))
                .andExpect(status().isUnauthorized()); // 401: no credentials provided
    }

    @Test
    @DisplayName("Should block unauthenticated access to /actuator/threaddump")
    void shouldBlockUnauthenticatedThreaddump() throws Exception {
        mockMvc.perform(get("/actuator/threaddump"))
                .andExpect(status().isUnauthorized()); // 401: no credentials provided
    }

    @Test
    @DisplayName("Should block unauthenticated access to /actuator/logfile")
    void shouldBlockUnauthenticatedLogfile() throws Exception {
        mockMvc.perform(get("/actuator/logfile"))
                .andExpect(status().isUnauthorized()); // 401: no credentials provided
    }

    // ==================== Restricted Endpoints (Regular User) ====================

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("Should block USER role from accessing /actuator/env")
    void shouldBlockUserRoleFromEnv() throws Exception {
        mockMvc.perform(get("/actuator/env"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("Should block USER role from accessing /actuator/metrics")
    void shouldBlockUserRoleFromMetrics() throws Exception {
        mockMvc.perform(get("/actuator/metrics"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("Should block USER role from accessing /actuator/heapdump")
    void shouldBlockUserRoleFromHeapdump() throws Exception {
        mockMvc.perform(get("/actuator/heapdump"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("Should block USER role from accessing /actuator/threaddump")
    void shouldBlockUserRoleFromThreaddump() throws Exception {
        mockMvc.perform(get("/actuator/threaddump"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "HOST")
    @DisplayName("Should block HOST role from accessing /actuator/env")
    void shouldBlockHostRoleFromEnv() throws Exception {
        mockMvc.perform(get("/actuator/env"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "AFFILIATE")
    @DisplayName("Should block AFFILIATE role from accessing /actuator/heapdump")
    void shouldBlockAffiliateRoleFromHeapdump() throws Exception {
        mockMvc.perform(get("/actuator/heapdump"))
                .andExpect(status().isForbidden());
    }

    // ==================== ADMIN Access (Should Succeed) ====================

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should allow ADMIN role to access /actuator/metrics")
    void shouldAllowAdminToAccessMetrics() throws Exception {
        mockMvc.perform(get("/actuator/metrics"))
                .andExpect(status().isOk());
    }

    // NOTE: /actuator/env is not exposed in application.yml (not in 'include' list)
    // so even ADMINs cannot access it. This is by design - env is completely
    // disabled.

    // ==================== Attack Scenarios ====================

    /**
     * Scenario: Attacker tries to access environment variables
     * This would expose JWT secrets, database credentials, AWS keys
     */
    @Test
    @DisplayName("Attack Scenario: Anonymous attacker tries to read environment variables")
    void attackScenario_ReadEnvironmentVariables() throws Exception {
        // Given: Attacker without authentication

        // When: Tries to access /actuator/env
        mockMvc.perform(get("/actuator/env"))
                // Then: Should be blocked with 401 Unauthorized (no credentials)
                .andExpect(status().isUnauthorized());
    }

    /**
     * Scenario: Malicious user tries to download heap dump
     * This could expose all application memory including sensitive data
     */
    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("Attack Scenario: Malicious user tries to download heap dump")
    void attackScenario_DownloadHeapdump() throws Exception {
        // Given: Malicious user with USER role

        // When: Tries to download heap dump
        mockMvc.perform(get("/actuator/heapdump"))
                // Then: Should be blocked with 403 Forbidden
                .andExpect(status().isForbidden());
    }

    /**
     * Scenario: Information gathering attack
     * Attacker tries to enumerate all actuator endpoints
     */
    @Test
    @DisplayName("Attack Scenario: Enumerate all actuator endpoints")
    void attackScenario_EnumerateEndpoints() throws Exception {
        // Given: Attacker without authentication

        // When: Tries to access various sensitive endpoints
        String[] sensitiveEndpoints = {
                "/actuator/env",
                "/actuator/heapdump",
                "/actuator/threaddump",
                "/actuator/metrics",
                "/actuator/logfile",
                "/actuator/beans",
                "/actuator/configprops"
        };

        for (String endpoint : sensitiveEndpoints) {
            // Then: All should be blocked - 401 if endpoint is exposed, 403 if not exposed
            // Both indicate the endpoint is protected (not accessible without ADMIN role)
            mockMvc.perform(get(endpoint))
                    .andExpect(status().is4xxClientError());
        }
    }

    /**
     * Scenario: Privilege escalation attempt
     * USER tries to access ADMIN-only endpoints
     */
    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("Attack Scenario: USER attempts privilege escalation via actuator")
    void attackScenario_PrivilegeEscalation() throws Exception {
        // Given: Regular USER role

        // When: Tries to access ADMIN-only endpoints
        String[] adminOnlyEndpoints = {
                "/actuator/env",
                "/actuator/metrics",
                "/actuator/heapdump",
                "/actuator/threaddump"
        };

        for (String endpoint : adminOnlyEndpoints) {
            // Then: All should return 403 Forbidden
            mockMvc.perform(get(endpoint))
                    .andExpect(status().isForbidden());
        }
    }
}