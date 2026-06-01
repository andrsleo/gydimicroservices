package com.affiliate.rentals.gydi.collaborations.infrastructure.in.rest.controller;

import com.affiliate.rentals.gydi.collaborations.infrastructure.in.rest.dto.UpdateCollaborationSettingsRequest;
import com.affiliate.rentals.gydi.properties.domain.model.Property;
import com.affiliate.rentals.gydi.properties.domain.model.PropertyId;
import com.affiliate.rentals.gydi.properties.domain.ports.out.PropertyRepositoryPort;
import com.affiliate.rentals.gydi.shared.security.OwnershipValidator;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST controller for managing property collaboration settings.
 *
 * <p>Only the property host may read or update collaboration settings for their property.
 *
 * @author GYDI Development Team
 */
@RestController
@RequestMapping("/api/v1/collaborations/settings")
@RequiredArgsConstructor
public class CollaborationSettingsController {

    private final PropertyRepositoryPort propertyRepository;
    private final OwnershipValidator ownershipValidator;

    /**
     * GET /api/v1/collaborations/settings/properties/{propertyId}
     * <p>Returns the collaboration settings for a property.
     */
    @GetMapping("/properties/{propertyId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> getSettings(@PathVariable Long propertyId) {
        Long userId = ownershipValidator.getAuthenticatedUserId();

        Property property = propertyRepository.findById(PropertyId.of(propertyId))
                .orElseThrow(() -> new IllegalArgumentException("Property not found: " + propertyId));

        if (!property.getHostId().equals(userId) && !ownershipValidator.isAdmin()) {
            throw new com.affiliate.rentals.gydi.collaborations.domain.exception
                    .CollaborationAccessDeniedException("Only the host may view collaboration settings");
        }

        return ResponseEntity.ok(Map.of(
                "propertyId", propertyId,
                "acceptCreatorCollaborations", property.isAcceptCreatorCollaborations(),
                "acceptedCompensations", property.getAcceptedCompensations()
        ));
    }

    /**
     * PUT /api/v1/collaborations/settings/properties/{propertyId}
     * <p>Updates the collaboration settings for a property.
     * Property must be in PUBLISHED status to enable collaborations.
     */
    @PutMapping("/properties/{propertyId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> updateSettings(
            @PathVariable Long propertyId,
            @Valid @RequestBody UpdateCollaborationSettingsRequest request) {

        Long userId = ownershipValidator.getAuthenticatedUserId();

        Property property = propertyRepository.findById(PropertyId.of(propertyId))
                .orElseThrow(() -> new IllegalArgumentException("Property not found: " + propertyId));

        if (!property.getHostId().equals(userId) && !ownershipValidator.isAdmin()) {
            throw new com.affiliate.rentals.gydi.collaborations.domain.exception
                    .CollaborationAccessDeniedException("Only the host may update collaboration settings");
        }

        property.updateCollaborationSettings(
                request.acceptCollaborations(),
                request.acceptedCompensations());

        propertyRepository.save(property);

        return ResponseEntity.ok(Map.of(
                "propertyId", propertyId,
                "acceptCreatorCollaborations", request.acceptCollaborations(),
                "acceptedCompensations", request.acceptedCompensations()
        ));
    }
}
