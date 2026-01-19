package com.affiliate.rentals.gydi.subscriptions.infrastructure.in.rest.controller;

import com.affiliate.rentals.gydi.shared.security.JwtService;
import com.affiliate.rentals.gydi.subscriptions.application.dto.*;
import com.affiliate.rentals.gydi.subscriptions.application.usecase.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for user subscription operations.
 *
 * <p>
 * This controller exposes endpoints for managing user subscriptions, including
 * creating subscriptions, changing plans, and canceling subscriptions.
 * </p>
 *
 * <p>
 * <b>Endpoints:</b>
 * </p>
 * <ul>
 * <li>GET /api/subscriptions/current - Get current user's subscription</li>
 * <li>POST /api/subscriptions - Subscribe to a plan</li>
 * <li>PUT /api/subscriptions/change-plan - Change subscription plan</li>
 * <li>POST /api/subscriptions/cancel - Cancel subscription</li>
 * </ul>
 *
 * <p>
 * <b>Access:</b> Requires authentication
 * </p>
 *
 * @author GYDI Development Team
 */
@RestController
@RequestMapping("/api/subscriptions")
@Tag(name = "User Subscriptions", description = "Endpoints for managing user subscriptions")
@SecurityRequirement(name = "Bearer Authentication")
public class SubscriptionController {

    private final GetUserSubscriptionUseCase getUserSubscriptionUseCase;
    private final SubscribeToPlanUseCase subscribeToPlanUseCase;
    private final ChangePlanUseCase changePlanUseCase;
    private final CancelSubscriptionUseCase cancelSubscriptionUseCase;
    private final JwtService jwtService;

    public SubscriptionController(
            GetUserSubscriptionUseCase getUserSubscriptionUseCase,
            SubscribeToPlanUseCase subscribeToPlanUseCase,
            ChangePlanUseCase changePlanUseCase,
            CancelSubscriptionUseCase cancelSubscriptionUseCase,
            JwtService jwtService) {
        this.getUserSubscriptionUseCase = getUserSubscriptionUseCase;
        this.subscribeToPlanUseCase = subscribeToPlanUseCase;
        this.changePlanUseCase = changePlanUseCase;
        this.cancelSubscriptionUseCase = cancelSubscriptionUseCase;
        this.jwtService = jwtService;
    }

    /**
     * Retrieves the authenticated user's current subscription.
     *
     * <p>
     * <b>Example response:</b>
     * </p>
     * 
     * <pre>
     * {
     *   "id": 123,
     *   "userId": 456,
     *   "planCode": "PRO",
     *   "planName": "Pro Plan",
     *   "status": "ACTIVE",
     *   "startedAt": "2024-01-01T00:00:00",
     *   "expiresAt": "2024-02-01T00:00:00",
     *   "nextBillingDate": "2024-02-01T00:00:00",
     *   "autoRenew": true
     * }
     * </pre>
     *
     * @param authentication the authenticated user
     * @return the user's current subscription
     */
    @GetMapping("/current")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get current subscription", description = "Retrieves the authenticated user's current subscription details.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Subscription retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "User not authenticated"),
            @ApiResponse(responseCode = "404", description = "User has no active subscription"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<UserSubscriptionResponse> getCurrentSubscription(
            Authentication authentication,
            HttpServletRequest request) {
        Long userId = extractUserId(request);
        UserSubscriptionResponse subscription = getUserSubscriptionUseCase.execute(userId);
        return ResponseEntity.ok(subscription);
    }

    /**
     * Subscribes the authenticated user to a plan.
     *
     * <p>
     * <b>Example request:</b>
     * </p>
     * 
     * <pre>
     * {
     *   "planCode": "PRO",
     *   "paymentMethodId": 789,
     *   "autoRenew": true
     * }
     * </pre>
     *
     * @param request        the subscription request
     * @param authentication the authenticated user
     * @return the created subscription
     */
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Subscribe to a plan", description = "Creates a new subscription for the authenticated user. "
            +
            "For paid plans, a valid payment method ID must be provided.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Subscription created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request or user already has active subscription"),
            @ApiResponse(responseCode = "401", description = "User not authenticated"),
            @ApiResponse(responseCode = "404", description = "Plan not found or payment method not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<UserSubscriptionResponse> subscribeToPlan(
            @Valid @RequestBody SubscribeToPlanRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest) {
        Long userId = extractUserId(httpRequest);
        UserSubscriptionResponse subscription = subscribeToPlanUseCase.execute(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(subscription);
    }

    /**
     * Changes the authenticated user's subscription plan.
     *
     * <p>
     * <b>Example request:</b>
     * </p>
     * 
     * <pre>
     * {
     *   "newPlanCode": "ELITE",
     *   "paymentMethodId": 789
     * }
     * </pre>
     *
     * @param request        the plan change request
     * @param authentication the authenticated user
     * @return the updated subscription
     */
    @PutMapping("/change-plan")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Change subscription plan", description = "Changes the authenticated user's subscription plan. "
            +
            "Upgrades are applied immediately with prorated payment. " +
            "Downgrades are applied at the end of the current billing period.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Plan changed successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid plan transition or payment method required"),
            @ApiResponse(responseCode = "401", description = "User not authenticated"),
            @ApiResponse(responseCode = "404", description = "Subscription, plan, or payment method not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<UserSubscriptionResponse> changePlan(
            @Valid @RequestBody ChangePlanRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest) {
        Long userId = extractUserId(httpRequest);
        UserSubscriptionResponse subscription = changePlanUseCase.execute(userId, request);
        return ResponseEntity.ok(subscription);
    }

    /**
     * Cancels the authenticated user's subscription.
     *
     * <p>
     * <b>Example request:</b>
     * </p>
     * 
     * <pre>
     * {
     *   "reason": "Too expensive",
     *   "cancelImmediately": false
     * }
     * </pre>
     *
     * @param request        the cancellation request
     * @param authentication the authenticated user
     * @return the canceled subscription
     */
    @PostMapping("/cancel")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Cancel subscription", description = "Cancels the authenticated user's subscription. " +
            "If cancelImmediately is true, the subscription is canceled now. " +
            "If false, it continues until the end of the current billing period.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Subscription canceled successfully"),
            @ApiResponse(responseCode = "400", description = "Subscription already canceled or invalid state"),
            @ApiResponse(responseCode = "401", description = "User not authenticated"),
            @ApiResponse(responseCode = "404", description = "User has no active subscription"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<UserSubscriptionResponse> cancelSubscription(
            @Valid @RequestBody CancelSubscriptionRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest) {
        Long userId = extractUserId(httpRequest);
        UserSubscriptionResponse subscription = cancelSubscriptionUseCase.execute(userId, request);
        return ResponseEntity.ok(subscription);
    }

    /**
     * Extracts the user ID from the JWT token in the request.
     *
     * @param request the HTTP servlet request
     * @return the user ID
     */
    private Long extractUserId(HttpServletRequest request) {
        return jwtService.extractUserIdFromRequest(request);
    }
}
