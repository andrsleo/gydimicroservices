package com.affiliate.rentals.gydi.collaborations.infrastructure.in.rest.controller;

import com.affiliate.rentals.gydi.collaborations.application.usecase.GetCollaborationMarketplaceUseCase;
import com.affiliate.rentals.gydi.properties.domain.model.Property;
import com.affiliate.rentals.gydi.properties.domain.ports.out.PropertyRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Public REST controller for the Creator Collaboration Marketplace.
 *
 * <p>All endpoints here are publicly accessible — no authentication required.
 * Authenticated endpoints for managing collaboration settings live in
 * {@link CollaborationSettingsController}.
 *
 * @author GYDI Development Team
 */
@RestController
@RequestMapping("/api/v1/collaborations/marketplace")
@RequiredArgsConstructor
public class CollaborationMarketplaceController {

    private final GetCollaborationMarketplaceUseCase getMarketplaceUseCase;

    /**
     * GET /api/v1/collaborations/marketplace
     * <p>Returns a paginated list of published properties that accept creator collaborations.
     *
     * @param compensationType optional filter (FREE_STAY, CASH, HYBRID, AFFILIATE, EXPERIENCE_EXCHANGE)
     * @param page             0-based page index
     * @param size             page size
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getMarketplace(
            @RequestParam(required = false) String compensationType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {

        PropertyRepositoryPort.PropertySearchResult result =
                getMarketplaceUseCase.execute(compensationType, page, size);

        List<Map<String, Object>> properties = result.properties().stream()
                .map(CollaborationMarketplaceController::toMarketplaceItem)
                .toList();

        return ResponseEntity.ok(Map.of(
                "properties", properties,
                "totalElements", result.totalElements(),
                "totalPages", result.totalPages(),
                "currentPage", result.currentPage(),
                "pageSize", result.pageSize()
        ));
    }

    private static Map<String, Object> toMarketplaceItem(Property p) {
        String country = p.getLocation() != null ? p.getLocation().country() : "";
        String city = p.getLocation() != null ? p.getLocation().city() : "";
        String thumbnailUrl = p.getImages() != null && !p.getImages().isEmpty()
                ? p.getImages().get(0).getUrl()
                : "";

        return Map.of(
                "id", p.getId().getValue(),
                "title", p.getTitle(),
                "slug", p.getSlug() != null ? p.getSlug() : "",
                "country", country != null ? country : "",
                "city", city != null ? city : "",
                "thumbnailUrl", thumbnailUrl,
                "acceptedCompensations", p.getAcceptedCompensations()
        );
    }
}
