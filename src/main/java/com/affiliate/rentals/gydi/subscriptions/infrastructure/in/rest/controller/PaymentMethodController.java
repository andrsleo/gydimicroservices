package com.affiliate.rentals.gydi.subscriptions.infrastructure.in.rest.controller;

import com.affiliate.rentals.gydi.shared.security.JwtService;
import com.affiliate.rentals.gydi.subscriptions.application.dto.CreatePaymentMethodRequest;
import com.affiliate.rentals.gydi.subscriptions.application.dto.PaymentMethodResponse;
import com.affiliate.rentals.gydi.subscriptions.application.usecase.CreatePaymentMethodUseCase;
import com.affiliate.rentals.gydi.subscriptions.application.usecase.DeletePaymentMethodUseCase;
import com.affiliate.rentals.gydi.subscriptions.application.usecase.GetUserPaymentMethodsUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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

import java.util.List;

/**
 * REST controller for payment method operations.
 *
 * <p>
 * This controller exposes endpoints for managing user payment methods.
 * Payment methods are tokenized using Stripe and stored securely.
 * </p>
 *
 * <p>
 * <b>Security:</b> This controller NEVER handles raw card data.
 * The frontend must tokenize payment details using Stripe Elements
 * before sending to this API.
 * </p>
 *
 * <p>
 * <b>Endpoints:</b>
 * </p>
 * <ul>
 * <li>GET /api/payment-methods - Get user's payment methods</li>
 * <li>POST /api/payment-methods - Add a new payment method</li>
 * </ul>
 *
 * <p>
 * <b>Access:</b> Requires authentication
 * </p>
 *
 * @author GYDI Development Team
 */
@RestController
@RequestMapping("/api/payment-methods")
@Tag(name = "Payment Methods", description = "Endpoints for managing user payment methods")
@SecurityRequirement(name = "Bearer Authentication")
public class PaymentMethodController {

    private final GetUserPaymentMethodsUseCase getUserPaymentMethodsUseCase;
    private final CreatePaymentMethodUseCase createPaymentMethodUseCase;
    private final DeletePaymentMethodUseCase deletePaymentMethodUseCase;
    private final JwtService jwtService;

    public PaymentMethodController(
            GetUserPaymentMethodsUseCase getUserPaymentMethodsUseCase,
            CreatePaymentMethodUseCase createPaymentMethodUseCase,
            DeletePaymentMethodUseCase deletePaymentMethodUseCase,
            JwtService jwtService) {
        this.getUserPaymentMethodsUseCase = getUserPaymentMethodsUseCase;
        this.createPaymentMethodUseCase = createPaymentMethodUseCase;
        this.deletePaymentMethodUseCase = deletePaymentMethodUseCase;
        this.jwtService = jwtService;
    }

    /**
     * Retrieves all payment methods for the authenticated user.
     *
     * <p>
     * Returns only active payment methods, sorted with the default method first.
     * </p>
     *
     * <p>
     * <b>Example response:</b>
     * </p>
     * 
     * <pre>
     * [
     *   {
     *     "id": 123,
     *     "methodType": "STRIPE",
     *     "cardBrand": "Visa",
     *     "cardLastFour": "4242",
     *     "cardExpMonth": 12,
     *     "cardExpYear": 2025,
     *     "isDefault": true,
     *     "isActive": true,
     *     "createdAt": "2024-01-01T00:00:00"
     *   }
     * ]
     * </pre>
     *
     * @param authentication the authenticated user
     * @return list of user's payment methods
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get user's payment methods", description = "Retrieves all active payment methods for the authenticated user. "
            +
            "Only returns last 4 digits and expiration date - never full card numbers.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Payment methods retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "User not authenticated"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<List<PaymentMethodResponse>> getUserPaymentMethods(
            Authentication authentication,
            HttpServletRequest request) {
        Long userId = extractUserId(request);
        List<PaymentMethodResponse> paymentMethods = getUserPaymentMethodsUseCase.execute(userId);
        return ResponseEntity.ok(paymentMethods);
    }

    /**
     * Adds a new payment method for the authenticated user.
     *
     * <p>
     * <b>IMPORTANT:</b> The stripePaymentMethodId must be obtained by tokenizing
     * the card details on the frontend using Stripe Elements. This API NEVER
     * receives
     * raw card data.
     * </p>
     *
     * <p>
     * <b>Example request:</b>
     * </p>
     * 
     * <pre>
     * {
     *   "stripePaymentMethodId": "pm_1Abc123...",
     *   "billingEmail": "user@example.com",
     *   "isDefault": true
     * }
     * </pre>
     *
     * <p>
     * <b>Frontend Integration:</b>
     * </p>
     * 
     * <pre>
     * // 1. Frontend creates Stripe payment method
     * const {paymentMethod} = await stripe.createPaymentMethod({
     *   type: 'card',
     *   card: cardElement,
     *   billing_details: { email: 'user@example.com' }
     * });
     *
     * // 2. Send token to backend
     * await fetch('/api/payment-methods', {
     *   method: 'POST',
     *   body: JSON.stringify({
     *     stripePaymentMethodId: paymentMethod.id,
     *     billingEmail: 'user@example.com',
     *     isDefault: true
     *   })
     * });
     * </pre>
     *
     * @param request        the payment method creation request
     * @param authentication the authenticated user
     * @return the created payment method
     */
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Add a new payment method", description = "Adds a new payment method for the authenticated user. "
            +
            "The payment method must be tokenized on the frontend using Stripe Elements. " +
            "If this is the first payment method, it's automatically set as default.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Payment method created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid Stripe payment method ID or validation error"),
            @ApiResponse(responseCode = "401", description = "User not authenticated"),
            @ApiResponse(responseCode = "500", description = "Internal server error or Stripe API error")
    })
    public ResponseEntity<PaymentMethodResponse> createPaymentMethod(
            @Valid @RequestBody CreatePaymentMethodRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest) {
        Long userId = extractUserId(httpRequest);
        PaymentMethodResponse paymentMethod = createPaymentMethodUseCase.execute(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(paymentMethod);
    }

    /**
     * Deletes a payment method.
     *
     * <p>
     * <b>Business Rules:</b>
     * </p>
     * <ul>
     * <li>Cannot delete the only payment method if user has a paid subscription
     * (PRO, ELITE)</li>
     * <li>Cannot delete default payment method without changing default first</li>
     * <li>Payment method must belong to authenticated user</li>
     * <li>Payment method is detached from Stripe before deletion</li>
     * </ul>
     *
     * <p>
     * <b>Example scenario (rejection):</b>
     * </p>
     * 
     * <pre>
     * User has:
     * - Active PRO subscription ($29/month)
     * - 1 payment method (Visa ending in 4242)
     *
     * DELETE /api/payment-methods/123
     * Response: 400 Bad Request
     * {
     *   "error": "Cannot Delete Payment Method",
     *   "message": "Cannot delete the only payment method while you have an active paid subscription..."
     * }
     * </pre>
     *
     * @param id             the payment method ID to delete
     * @param authentication the authenticated user
     * @param httpRequest    the HTTP servlet request
     * @return 204 No Content if deletion successful
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Delete a payment method", description = "Deletes a payment method for the authenticated user. "
            +
            "Business rules prevent deletion if it would violate subscription payment requirements. " +
            "The payment method is detached from Stripe before deletion.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Payment method deleted successfully"),
            @ApiResponse(responseCode = "400", description = "Cannot delete payment method due to business rules"),
            @ApiResponse(responseCode = "401", description = "User not authenticated"),
            @ApiResponse(responseCode = "404", description = "Payment method not found or doesn't belong to user"),
            @ApiResponse(responseCode = "500", description = "Internal server error or Stripe API error")
    })
    public ResponseEntity<Void> deletePaymentMethod(
            @Parameter(description = "Payment method ID", required = true) @PathVariable Long id,
            Authentication authentication,
            HttpServletRequest httpRequest) {
        Long userId = extractUserId(httpRequest);

        deletePaymentMethodUseCase.execute(id, userId);

        return ResponseEntity.noContent().build();
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
