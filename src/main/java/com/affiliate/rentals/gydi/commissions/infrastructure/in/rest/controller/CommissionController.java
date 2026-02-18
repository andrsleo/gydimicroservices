package com.affiliate.rentals.gydi.commissions.infrastructure.in.rest.controller;

import com.affiliate.rentals.gydi.commissions.application.dto.ReferralCommissionDto;
import com.affiliate.rentals.gydi.commissions.application.dto.CommissionStatsDto;
import com.affiliate.rentals.gydi.commissions.application.dto.HostCommissionDto;
import com.affiliate.rentals.gydi.commissions.application.mapper.HostCommissionMapper;
import com.affiliate.rentals.gydi.commissions.application.usecase.ChargeHostCommissionUseCase;
import com.affiliate.rentals.gydi.commissions.application.usecase.GetHostCommissionsByUserUseCase;
import com.affiliate.rentals.gydi.commissions.domain.ports.ReferralCommissionRepositoryPort;
import com.affiliate.rentals.gydi.shared.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * REST Controller for commission management.
 */
@RestController("commissionManagementController")
@RequestMapping("/api/v1/commissions")
@Tag(name = "Commissions", description = "Commission management endpoints")
public class CommissionController {

    private final GetHostCommissionsByUserUseCase getHostCommissionsUseCase;
    private final ReferralCommissionRepositoryPort affiliateCommissionRepository;
    private final ChargeHostCommissionUseCase chargeHostCommissionUseCase;
    private final HostCommissionMapper hostCommissionMapper;

    public CommissionController(
            GetHostCommissionsByUserUseCase getHostCommissionsUseCase,
            ReferralCommissionRepositoryPort affiliateCommissionRepository,
            ChargeHostCommissionUseCase chargeHostCommissionUseCase,
            HostCommissionMapper hostCommissionMapper) {
        this.getHostCommissionsUseCase = getHostCommissionsUseCase;
        this.affiliateCommissionRepository = affiliateCommissionRepository;
        this.chargeHostCommissionUseCase = chargeHostCommissionUseCase;
        this.hostCommissionMapper = hostCommissionMapper;
    }

    // ========================================
    // AFFILIATE ENDPOINTS (must come BEFORE /{id})
    // SEMANTIC: Commissions EARNED (platform PAYS affiliate)
    // ========================================
    // TODO: Implement proper role assignment based on subscription plans
    // Currently allowing ROLE_USER temporarily to unblock frontend
    // Proper implementation should assign ROLE_AFFILIATE when user subscribes to a
    // plan

    @GetMapping("/affiliate/earned")
    @PreAuthorize("hasRole('USER') or hasRole('AFFILIATE') or hasRole('ADMIN')")
    @Operation(summary = "Get affiliate commissions earned", description = "Returns commissions earned by affiliate (platform PAYS affiliate)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Affiliate commissions retrieved"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<List<ReferralCommissionDto>> getAffiliateCommissionsEarned(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        // ✅ SECURITY FIX: Extract actual user ID from JWT authentication
        Long userId = userDetails.getUserId();

        // TODO: Implement full mapping with AffiliateCommissionMapper
        // For now, return empty list to prevent 404 errors
        return ResponseEntity.ok(List.of());
    }

    @GetMapping("/affiliate/stats")
    @PreAuthorize("hasRole('USER') or hasRole('AFFILIATE') or hasRole('ADMIN')")
    @Operation(summary = "Get affiliate commission statistics", description = "Returns stats for commissions earned by affiliate")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Stats retrieved"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<CommissionStatsDto> getAffiliateStats(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long userId = userDetails.getUserId();

        // Get all affiliate commissions for stats
        var commissions = affiliateCommissionRepository.findByAffiliateId(userId);

        BigDecimal totalEarned = commissions.stream()
                .filter(c -> c.getStatus().name().equals("PAID"))
                .map(c -> c.getAmount().getCommissionAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal pending = commissions.stream()
                .filter(c -> c.getStatus().name().equals("PENDING"))
                .map(c -> c.getAmount().getCommissionAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        String currency = commissions.stream()
                .findFirst()
                .map(c -> c.getAmount().getCurrency())
                .orElse("USD");

        return ResponseEntity.ok(new CommissionStatsDto(totalEarned, BigDecimal.ZERO, pending, currency));
    }

    // ========================================
    // HOST ENDPOINTS (must come BEFORE /{id})
    // SEMANTIC: Commissions PAID (platform CHARGES host)
    // ========================================

    @GetMapping("/host/paid")
    @PreAuthorize("hasRole('USER') or hasRole('HOST') or hasRole('ADMIN')")
    @Operation(summary = "Get host commissions paid", description = "Returns commissions paid by host (platform CHARGES host)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Host commissions retrieved"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<List<HostCommissionDto>> getHostCommissionsPaid(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        // ✅ SECURITY FIX: Extract actual user ID from JWT authentication
        Long userId = userDetails.getUserId();
        List<HostCommissionDto> commissions = getHostCommissionsUseCase.execute(userId);
        return ResponseEntity.ok(commissions);
    }

    @GetMapping("/host/stats")
    @PreAuthorize("hasRole('USER') or hasRole('HOST') or hasRole('ADMIN')")
    @Operation(summary = "Get host commission statistics", description = "Returns stats for commissions paid by host")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Stats retrieved"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<CommissionStatsDto> getHostStats(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long userId = userDetails.getUserId();

        // Get all host commissions for stats
        List<HostCommissionDto> commissions = getHostCommissionsUseCase.execute(userId);

        BigDecimal totalPaid = commissions.stream()
                .filter(c -> c.status().equals("PAID"))
                .map(HostCommissionDto::commissionAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal pending = commissions.stream()
                .filter(c -> c.status().equals("PENDING"))
                .map(HostCommissionDto::commissionAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        String currency = commissions.stream()
                .findFirst()
                .map(HostCommissionDto::currency)
                .orElse("USD");

        return ResponseEntity.ok(new CommissionStatsDto(BigDecimal.ZERO, totalPaid, pending, currency));
    }

    @PostMapping("/admin/charge/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Manually trigger host commission charge (ADMIN)")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Charge triggered"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Admin only"),
            @ApiResponse(responseCode = "404", description = "Commission not found")
    })
    public ResponseEntity<Void> chargeHostCommission(@PathVariable Long id) {
        chargeHostCommissionUseCase.execute(id);
        return ResponseEntity.noContent().build();
    }
}
