package com.affiliate.rentals.gydi.content.infrastructure.in.rest.controller;

import com.affiliate.rentals.gydi.content.application.dto.AttributionFunnelDto;
import com.affiliate.rentals.gydi.content.application.dto.CreatorAnalyticsSummaryDto;
import com.affiliate.rentals.gydi.content.application.dto.PlatformAnalyticsDto;
import com.affiliate.rentals.gydi.content.application.usecase.GetAttributionFunnelUseCase;
import com.affiliate.rentals.gydi.content.application.usecase.GetCreatorLeaderboardUseCase;
import com.affiliate.rentals.gydi.content.application.usecase.GetPlatformAnalyticsUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin-only REST controller for platform analytics.
 *
 * <p>All endpoints require the {@code ADMIN} role. Authorization is enforced both
 * via {@code @PreAuthorize} at method level and via the SecurityConfig path matcher
 * {@code /api/v1/admin/analytics/**}.</p>
 *
 * <p>Delegates entirely to use cases — no business logic resides in this controller.</p>
 *
 * @author GYDI Development Team
 */
@RestController
@RequestMapping("/api/v1/admin/analytics")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin Analytics", description = "Platform analytics endpoints — ADMIN only")
public class AdminAnalyticsController {

    private final GetPlatformAnalyticsUseCase getPlatformAnalyticsUseCase;
    private final GetCreatorLeaderboardUseCase getCreatorLeaderboardUseCase;
    private final GetAttributionFunnelUseCase getAttributionFunnelUseCase;

    /**
     * Returns platform-wide metrics: GMV, viral coefficient, content velocity,
     * total creators, total views and total published posts.
     */
    @GetMapping("/platform")
    @Operation(
            summary = "Platform analytics",
            description = "Returns GMV, viral coefficient, content velocity, total creators, total views and total posts."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Analytics retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthenticated"),
            @ApiResponse(responseCode = "403", description = "Insufficient privileges — ADMIN role required")
    })
    public ResponseEntity<PlatformAnalyticsDto> getPlatformAnalytics() {
        return ResponseEntity.ok(getPlatformAnalyticsUseCase.execute());
    }

    /**
     * Returns creator analytics: top 20 creators by total views, new creators this month,
     * and 30-day creator retention rate.
     */
    @GetMapping("/creators")
    @Operation(
            summary = "Creator analytics",
            description = "Top 20 creators from the creator_leaderboard materialized view, plus retention metrics."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Creator analytics retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthenticated"),
            @ApiResponse(responseCode = "403", description = "Insufficient privileges — ADMIN role required")
    })
    public ResponseEntity<CreatorAnalyticsSummaryDto> getCreatorAnalytics() {
        return ResponseEntity.ok(getCreatorLeaderboardUseCase.execute());
    }

    /**
     * Returns the content-to-booking attribution funnel: total bookings from content,
     * conversion rate, average attributed revenue, total views and property-linked posts.
     */
    @GetMapping("/attribution")
    @Operation(
            summary = "Attribution funnel",
            description = "Content-to-booking attribution metrics from referrals.content_attributions."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Attribution funnel retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthenticated"),
            @ApiResponse(responseCode = "403", description = "Insufficient privileges — ADMIN role required")
    })
    public ResponseEntity<AttributionFunnelDto> getAttributionFunnel() {
        return ResponseEntity.ok(getAttributionFunnelUseCase.execute());
    }
}
