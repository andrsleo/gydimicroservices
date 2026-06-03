package com.affiliate.rentals.gydi.properties.infrastructure.in.rest.controller;

import com.affiliate.rentals.gydi.content.application.dto.PropertySocialProofDto;
import com.affiliate.rentals.gydi.content.application.usecase.GetPropertySocialProofUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for property social proof endpoints.
 * Exposes creator content stats for a property (Phase 4 Social Commerce).
 * Mounted under /api/v1/properties for versioning consistency.
 */
@RestController
@RequestMapping("/api/v1/properties")
@RequiredArgsConstructor
public class PropertySocialProofController {

    private final GetPropertySocialProofUseCase getPropertySocialProofUseCase;

    /**
     * GET /api/v1/properties/{id}/social-proof
     * Returns content count, total views, total bookings, and top creators for a property.
     * Public endpoint.
     */
    @GetMapping("/{id}/social-proof")
    public ResponseEntity<PropertySocialProofDto> getSocialProof(@PathVariable Long id) {
        return ResponseEntity.ok(getPropertySocialProofUseCase.execute(id));
    }
}
