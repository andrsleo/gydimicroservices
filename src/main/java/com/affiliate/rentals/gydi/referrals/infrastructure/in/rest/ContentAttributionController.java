package com.affiliate.rentals.gydi.referrals.infrastructure.in.rest;

import com.affiliate.rentals.gydi.referrals.application.dto.ContentAttributionDto;
import com.affiliate.rentals.gydi.referrals.application.dto.CreateContentAttributionRequest;
import com.affiliate.rentals.gydi.referrals.application.usecase.CreateContentAttributionUseCase;
import com.affiliate.rentals.gydi.referrals.application.usecase.GetContentAttributionsByCreatorUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/content-attributions")
@RequiredArgsConstructor
@Tag(name = "Content Attributions", description = "UGC content attribution for creator commissions")
public class ContentAttributionController {

    private final CreateContentAttributionUseCase createContentAttributionUseCase;
    private final GetContentAttributionsByCreatorUseCase getContentAttributionsByCreatorUseCase;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Create content attribution",
            description = "Links a booking to a content post for creator commission attribution")
    @ApiResponse(responseCode = "201", description = "Attribution created successfully")
    @ApiResponse(responseCode = "400", description = "Invalid request")
    public ResponseEntity<ContentAttributionDto> create(
            @Valid @RequestBody CreateContentAttributionRequest request) {
        ContentAttributionDto attribution = createContentAttributionUseCase.execute(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(attribution);
    }

    @GetMapping("/creator/{creatorId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get attributions by creator",
            description = "Retrieves all content attributions for a given creator")
    @ApiResponse(responseCode = "200", description = "Attributions retrieved successfully")
    public ResponseEntity<List<ContentAttributionDto>> getByCreator(
            @PathVariable Long creatorId) {
        List<ContentAttributionDto> attributions = getContentAttributionsByCreatorUseCase.execute(creatorId);
        return ResponseEntity.ok(attributions);
    }
}
