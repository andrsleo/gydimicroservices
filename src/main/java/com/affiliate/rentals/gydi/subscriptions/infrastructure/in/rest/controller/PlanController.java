package com.affiliate.rentals.gydi.subscriptions.infrastructure.in.rest.controller;

import com.affiliate.rentals.gydi.subscriptions.application.dto.PlanResponse;
import com.affiliate.rentals.gydi.subscriptions.application.usecase.GetAllActivePlansUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller for subscription plan operations.
 *
 * <p>
 * This controller exposes endpoints for retrieving subscription plans.
 * Plans define the pricing tiers and commission rates for the affiliate
 * program.
 * </p>
 *
 * <p>
 * <b>Endpoints:</b>
 * </p>
 * <ul>
 * <li>GET /api/plans - Get all active subscription plans</li>
 * </ul>
 *
 * <p>
 * <b>Access:</b> Public - No authentication required
 * </p>
 *
 * @author GYDI Development Team
 */
@RestController
@RequestMapping("/api/plans")
@Tag(name = "Subscription Plans", description = "Endpoints for managing subscription plans")
public class PlanController {

    private final GetAllActivePlansUseCase getAllActivePlansUseCase;

    public PlanController(GetAllActivePlansUseCase getAllActivePlansUseCase) {
        this.getAllActivePlansUseCase = getAllActivePlansUseCase;
    }

    /**
     * Retrieves all active subscription plans.
     *
     * <p>
     * Returns a list of all currently available subscription plans with their
     * pricing, commission rates, and feature limits.
     * </p>
     *
     * <p>
     * <b>Example response:</b>
     * </p>
     * 
     * <pre>
     * [
     *   {
     *     "id": 1,
     *     "planCode": "FREE",
     *     "planName": "Free Plan",
     *     "monthlyPrice": 0.00,
     *     "commissionRate": 0.02,
     *     "maxProperties": 10,
     *     "maxReferralsPerMonth": 50,
     *     "isActive": true
     *   },
     *   {
     *     "id": 2,
     *     "planCode": "PRO",
     *     "planName": "Pro Plan",
     *     "monthlyPrice": 29.99,
     *     "commissionRate": 0.05,
     *     "maxProperties": 50,
     *     "maxReferralsPerMonth": 200,
     *     "isActive": true
     *   }
     * ]
     * </pre>
     *
     * @return list of active subscription plans
     */
    @GetMapping
    @Operation(summary = "Get all active subscription plans", description = "Retrieves all currently available subscription plans with pricing and features. "
            +
            "This endpoint is public and does not require authentication.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Plans retrieved successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<List<PlanResponse>> getAllActivePlans() {
        List<PlanResponse> plans = getAllActivePlansUseCase.execute();
        return ResponseEntity.ok(plans);
    }
}
