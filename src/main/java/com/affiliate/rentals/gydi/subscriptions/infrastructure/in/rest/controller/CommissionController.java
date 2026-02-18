package com.affiliate.rentals.gydi.subscriptions.infrastructure.in.rest.controller;

import com.affiliate.rentals.gydi.subscriptions.application.dto.CalculateCommissionRequest;
import com.affiliate.rentals.gydi.subscriptions.application.dto.CommissionBreakdownResponse;
import com.affiliate.rentals.gydi.subscriptions.application.dto.CommissionRatesResponse;
import com.affiliate.rentals.gydi.subscriptions.application.usecase.CalculateCommissionUseCase;
import com.affiliate.rentals.gydi.subscriptions.application.usecase.GetCommissionRatesUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for commission calculation operations.
 *
 * <p><strong>Business Model:</strong></p>
 * <ul>
 *   <li><strong>AFFILIATE</strong>: RECEIVES commission FROM platform (2%, 5%, 10%)</li>
 *   <li><strong>HOST</strong>: PAYS commission TO platform (25%, 20%, 15%)</li>
 *   <li><strong>PLATFORM</strong>: Earns profit = (host fee - affiliate commission)</li>
 * </ul>
 *
 * <p><strong>Endpoints:</strong></p>
 * <ul>
 *   <li>POST /api/v1/commission-calculator/calculate - Calculate commission breakdown</li>
 *   <li>GET /api/v1/commission-calculator/rates - Get commission rates for all plans</li>
 * </ul>
 *
 * <p><strong>Access:</strong> Public - No authentication required for transparency</p>
 *
 * @author GYDI Development Team
 * @version 2.0
 * @since 2026-02-04
 */
@RestController("subscriptionCommissionCalculatorController")
@RequestMapping("/api/v1/commission-calculator")
@Tag(name = "Commission Calculator", description = "Endpoints for commission calculations and rates")
public class CommissionController {

    private final CalculateCommissionUseCase calculateCommissionUseCase;
    private final GetCommissionRatesUseCase getCommissionRatesUseCase;

    public CommissionController(
            CalculateCommissionUseCase calculateCommissionUseCase,
            GetCommissionRatesUseCase getCommissionRatesUseCase
    ) {
        this.calculateCommissionUseCase = calculateCommissionUseCase;
        this.getCommissionRatesUseCase = getCommissionRatesUseCase;
    }

    /**
     * Calculate commission breakdown for a booking transaction.
     *
     * <p>Given a booking amount and subscription plans for both host and affiliate,
     * this endpoint calculates the complete commission breakdown showing:</p>
     * <ul>
     *   <li>Platform fee charged to host</li>
     *   <li>Commission paid to affiliate</li>
     *   <li>Host's net income</li>
     *   <li>Platform's profit</li>
     * </ul>
     *
     * <p><strong>Example calculation (PRO plan, $100 booking):</strong></p>
     * <ul>
     *   <li>Platform charges host: $20 (20%)</li>
     *   <li>Platform pays affiliate: $5 (5%)</li>
     *   <li>Host receives: $80</li>
     *   <li>Platform profit: $15</li>
     * </ul>
     *
     * @param request The calculation request with booking amount and plan codes
     * @return Commission breakdown with all calculated amounts
     */
    @PostMapping("/calculate")
    @Operation(
            summary = "Calculate commission breakdown",
            description = "Calculates how commissions are distributed for a booking transaction based on subscription plans"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Commission breakdown calculated successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = CommissionBreakdownResponse.class),
                            examples = @ExampleObject(
                                    value = """
                                            {
                                              "booking_amount": 100.00,
                                              "platform_fee": 20.00,
                                              "affiliate_commission": 5.00,
                                              "host_net_income": 80.00,
                                              "platform_profit": 15.00,
                                              "host_plan": "PRO",
                                              "affiliate_plan": "PRO"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request (negative amount, invalid plan code, etc.)"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Host or affiliate plan not found"
            )
    })
    public ResponseEntity<CommissionBreakdownResponse> calculateCommission(
            @Valid @RequestBody CalculateCommissionRequest request
    ) {
        CommissionBreakdownResponse breakdown = calculateCommissionUseCase.execute(request);
        return ResponseEntity.ok(breakdown);
    }

    /**
     * Get commission rates for all subscription plans.
     *
     * <p>Returns commission rates for both affiliates and hosts across all
     * available subscription tiers (FREE, PRO, ELITE).</p>
     *
     * <p>This endpoint is useful for:</p>
     * <ul>
     *   <li>Displaying commission rates in the UI</li>
     *   <li>Building plan comparison tables</li>
     *   <li>Powering commission calculators</li>
     * </ul>
     *
     * @return Commission rates organized by role (affiliate vs host)
     */
    @GetMapping("/rates")
    @Operation(
            summary = "Get commission rates",
            description = "Retrieves commission rates for all subscription plans, organized by role"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Commission rates retrieved successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = CommissionRatesResponse.class),
                            examples = @ExampleObject(
                                    value = """
                                            {
                                              "affiliate_rates": {
                                                "FREE": { "plan": "FREE", "earns_percentage": 0.02, "display": "2%" },
                                                "PRO": { "plan": "PRO", "earns_percentage": 0.05, "display": "5%" },
                                                "ELITE": { "plan": "ELITE", "earns_percentage": 0.10, "display": "10%" }
                                              },
                                              "host_rates": {
                                                "FREE": { "plan": "FREE", "platform_charges_percentage": 0.25, "display": "25%" },
                                                "PRO": { "plan": "PRO", "platform_charges_percentage": 0.20, "display": "20%" },
                                                "ELITE": { "plan": "ELITE", "platform_charges_percentage": 0.15, "display": "15%" }
                                              }
                                            }
                                            """
                            )
                    )
            )
    })
    public ResponseEntity<CommissionRatesResponse> getCommissionRates() {
        CommissionRatesResponse rates = getCommissionRatesUseCase.execute();
        return ResponseEntity.ok(rates);
    }
}
