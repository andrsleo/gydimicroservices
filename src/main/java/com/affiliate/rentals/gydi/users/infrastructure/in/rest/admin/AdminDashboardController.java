package com.affiliate.rentals.gydi.users.infrastructure.in.rest.admin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin Dashboard Controller
 *
 * <p>Provides endpoints for admin dashboard metrics and statistics.
 * All endpoints require ADMIN role.</p>
 *
 * @author GYDI Development Team
 */
@RestController
@RequestMapping("/api/v1/admin/dashboard")
@Tag(name = "Admin - Dashboard", description = "Admin dashboard statistics endpoints")
@PreAuthorize("hasRole('ADMIN')")
public class AdminDashboardController {

    /**
     * Get dashboard statistics.
     *
     * <p>Returns global platform metrics for the admin dashboard including
     * user counts, property counts, commission totals, and system health.</p>
     *
     * @return DashboardStatsDto containing platform metrics
     */
    @GetMapping("/stats")
    @Operation(
            summary = "Get dashboard statistics",
            description = "Returns global platform metrics for admin dashboard. Requires ADMIN role."
    )
    public ResponseEntity<DashboardStatsDto> getDashboardStats() {
        // TODO: Implement actual statistics from database
        // For now, return placeholder data
        DashboardStatsDto stats = DashboardStatsDto.builder()
                .totalUsers(1247)
                .activeUsers(892)
                .totalProperties(3456)
                .activeProperties(2891)
                .totalCommissions(45678.90)
                .pendingCommissions(3456.78)
                .conversionRate(12.5)
                .systemHealth("healthy")
                .build();

        return ResponseEntity.ok(stats);
    }

    /**
     * Dashboard Statistics DTO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DashboardStatsDto {
        private Integer totalUsers;
        private Integer activeUsers;
        private Integer totalProperties;
        private Integer activeProperties;
        private Double totalCommissions;
        private Double pendingCommissions;
        private Double conversionRate;
        private String systemHealth;
    }
}
