package com.affiliate.rentals.gydi.users.infrastructure.in.rest.controller;

import com.affiliate.rentals.gydi.shared.security.OwnershipValidator;
import com.affiliate.rentals.gydi.users.application.dto.CreatorAnalyticsOverviewDto;
import com.affiliate.rentals.gydi.users.application.dto.CreatorContentAnalyticsDto;
import com.affiliate.rentals.gydi.users.application.dto.CreatorEarningsDto;
import com.affiliate.rentals.gydi.users.application.usecase.GetCreatorAnalyticsOverviewUseCase;
import com.affiliate.rentals.gydi.users.application.usecase.GetCreatorContentAnalyticsUseCase;
import com.affiliate.rentals.gydi.users.application.usecase.GetCreatorEarningsUseCase;
import com.affiliate.rentals.gydi.users.application.usecase.GetCreatorTopContentUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/creator/analytics")
@RequiredArgsConstructor
@Tag(name = "Creator Analytics", description = "Creator performance analytics and earnings breakdown")
public class CreatorAnalyticsController {

    private final GetCreatorAnalyticsOverviewUseCase analyticsOverviewUseCase;
    private final GetCreatorContentAnalyticsUseCase contentAnalyticsUseCase;
    private final GetCreatorTopContentUseCase topContentUseCase;
    private final GetCreatorEarningsUseCase earningsUseCase;
    private final OwnershipValidator ownershipValidator;

    @GetMapping("/overview")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Creator analytics overview",
            description = "Returns aggregated KPIs: views, likes, saves, bookings, earnings, followers")
    @ApiResponse(responseCode = "200", description = "Overview retrieved successfully")
    public ResponseEntity<CreatorAnalyticsOverviewDto> getOverview() {
        Long creatorId = ownershipValidator.getAuthenticatedUserId();
        return ResponseEntity.ok(analyticsOverviewUseCase.execute(creatorId));
    }

    @GetMapping("/content")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Content analytics list",
            description = "Returns creator's posts with per-post metrics: views, likes, bookings generated, earnings")
    @ApiResponse(responseCode = "200", description = "Content analytics retrieved successfully")
    public ResponseEntity<List<CreatorContentAnalyticsDto>> getContentAnalytics(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Long creatorId = ownershipValidator.getAuthenticatedUserId();
        return ResponseEntity.ok(contentAnalyticsUseCase.execute(creatorId, page, size));
    }

    @GetMapping("/earnings")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Earnings breakdown by content post",
            description = "Returns commission breakdown linked to content attributions")
    @ApiResponse(responseCode = "200", description = "Earnings retrieved successfully")
    public ResponseEntity<List<CreatorEarningsDto>> getEarnings() {
        Long creatorId = ownershipValidator.getAuthenticatedUserId();
        return ResponseEntity.ok(earningsUseCase.execute(creatorId));
    }

    @GetMapping("/top-content")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Top performing content",
            description = "Returns top N posts ranked by composite performance score (views + likes + bookings + saves)")
    @ApiResponse(responseCode = "200", description = "Top content retrieved successfully")
    public ResponseEntity<List<CreatorContentAnalyticsDto>> getTopContent(
            @RequestParam(defaultValue = "5") int limit) {
        Long creatorId = ownershipValidator.getAuthenticatedUserId();
        return ResponseEntity.ok(topContentUseCase.execute(creatorId, limit));
    }
}
