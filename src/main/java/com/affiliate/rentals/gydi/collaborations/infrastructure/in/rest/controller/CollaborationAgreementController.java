package com.affiliate.rentals.gydi.collaborations.infrastructure.in.rest.controller;

import com.affiliate.rentals.gydi.collaborations.application.usecase.*;
import com.affiliate.rentals.gydi.collaborations.application.usecase.command.*;
import com.affiliate.rentals.gydi.collaborations.domain.model.AgreementDeliverable;
import com.affiliate.rentals.gydi.collaborations.domain.model.CollaborationAgreement;
import com.affiliate.rentals.gydi.collaborations.domain.model.ContentRight;
import com.affiliate.rentals.gydi.collaborations.domain.model.DeliveryAsset;
import com.affiliate.rentals.gydi.collaborations.infrastructure.in.rest.dto.*;
import com.affiliate.rentals.gydi.shared.security.OwnershipValidator;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * REST controller for collaboration agreement lifecycle and content delivery.
 *
 * <p>Covers: view agreement, upload delivery asset, approve deliverable,
 * request revision, and cancel agreement.
 *
 * @author GYDI Development Team
 */
@RestController
@RequestMapping("/api/v1/collaborations/agreements")
@RequiredArgsConstructor
public class CollaborationAgreementController {

    private final GetAgreementUseCase getAgreementUseCase;
    private final UploadDeliveryAssetUseCase uploadDeliveryAssetUseCase;
    private final ApproveDeliverableUseCase approveDeliverableUseCase;
    private final RequestRevisionUseCase requestRevisionUseCase;
    private final CancelAgreementUseCase cancelAgreementUseCase;
    private final OwnershipValidator ownershipValidator;

    /**
     * GET /api/v1/collaborations/agreements/{agreementId}
     * <p>Returns full agreement detail. Only host or creator may access.
     */
    @GetMapping("/{agreementId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> getAgreement(@PathVariable Long agreementId) {
        Long userId = ownershipValidator.getAuthenticatedUserId();
        CollaborationAgreement agreement = getAgreementUseCase.execute(agreementId, userId);
        return ResponseEntity.ok(toAgreementDetail(agreement));
    }

    /**
     * POST /api/v1/collaborations/agreements/{agreementId}/deliverables/{deliverableId}/assets
     * <p>Register a pre-uploaded delivery asset for a specific deliverable.
     * File must be uploaded to storage first — this endpoint records the asset in the DB.
     */
    @PostMapping("/{agreementId}/deliverables/{deliverableId}/assets")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> uploadDeliveryAsset(
            @PathVariable Long agreementId,
            @PathVariable Long deliverableId,
            @Valid @RequestBody UploadDeliveryAssetRequest request) {
        Long userId = ownershipValidator.getAuthenticatedUserId();

        UploadDeliveryAssetCommand command = new UploadDeliveryAssetCommand(
                agreementId,
                deliverableId,
                userId,
                request.fileUrl(),
                request.fileType(),
                request.fileSizeBytes()
        );

        DeliveryAsset asset = uploadDeliveryAssetUseCase.execute(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(toAssetMap(asset));
    }

    /**
     * POST /api/v1/collaborations/agreements/{agreementId}/deliverables/{deliverableId}/approve
     * <p>Host approves a submitted deliverable. If all deliverables are approved,
     * the agreement transitions to COMPLETED.
     */
    @PostMapping("/{agreementId}/deliverables/{deliverableId}/approve")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApproveDeliverableResult> approveDeliverable(
            @PathVariable Long agreementId,
            @PathVariable Long deliverableId) {
        Long userId = ownershipValidator.getAuthenticatedUserId();
        ApproveDeliverableResult result = approveDeliverableUseCase.execute(
                agreementId, deliverableId, userId);
        return ResponseEntity.ok(result);
    }

    /**
     * POST /api/v1/collaborations/agreements/{agreementId}/deliverables/{deliverableId}/revision
     * <p>Host requests a revision on a submitted deliverable.
     */
    @PostMapping("/{agreementId}/deliverables/{deliverableId}/revision")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<RequestRevisionResult> requestRevision(
            @PathVariable Long agreementId,
            @PathVariable Long deliverableId,
            @Valid @RequestBody RequestRevisionRequest request) {
        Long userId = ownershipValidator.getAuthenticatedUserId();
        RequestRevisionResult result = requestRevisionUseCase.execute(
                agreementId, deliverableId, userId, request.feedback());
        return ResponseEntity.ok(result);
    }

    /**
     * POST /api/v1/collaborations/agreements/{agreementId}/cancel
     * <p>Cancel an active agreement. Either party (host or creator) may cancel.
     */
    @PostMapping("/{agreementId}/cancel")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> cancelAgreement(@PathVariable Long agreementId) {
        Long userId = ownershipValidator.getAuthenticatedUserId();
        cancelAgreementUseCase.execute(agreementId, userId);
        return ResponseEntity.noContent().build();
    }

    // ── Mappers ───────────────────────────────────────────────────────────────

    private static Map<String, Object> toAgreementDetail(CollaborationAgreement a) {
        List<Map<String, Object>> deliverables = a.deliverables().stream()
                .map(CollaborationAgreementController::toDeliverableMap)
                .collect(Collectors.toList());

        List<Map<String, Object>> rights = a.contentRights().stream()
                .map(CollaborationAgreementController::toRightMap)
                .collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("id", a.id());
        result.put("pitchId", a.pitchId());
        result.put("propertyId", a.propertyId());
        result.put("hostId", a.hostId());
        result.put("creatorId", a.creatorId());
        result.put("status", a.status().name());
        result.put("checkInDate", a.checkInDate().toString());
        result.put("checkOutDate", a.checkOutDate().toString());
        result.put("deliveryDeadline", a.deliveryDeadline().toString());
        result.put("postingDeadline", a.postingDeadline().toString());
        result.put("deliverables", deliverables);
        result.put("contentRights", rights);
        result.put("createdAt", a.createdAt().toString());
        return result;
    }

    private static Map<String, Object> toDeliverableMap(AgreementDeliverable d) {
        List<Map<String, Object>> assets = d.assets().stream()
                .map(CollaborationAgreementController::toAssetMap)
                .collect(Collectors.toList());

        return Map.of(
                "id", d.id(),
                "type", d.type() != null ? d.type().name() : "",
                "quantity", d.quantity(),
                "status", d.status() != null ? d.status() : "",
                "assets", assets
        );
    }

    private static Map<String, Object> toAssetMap(DeliveryAsset asset) {
        return Map.of(
                "id", asset.id(),
                "fileUrl", asset.fileUrl(),
                "fileType", asset.fileType(),
                "fileSizeBytes", asset.fileSizeBytes(),
                "uploadedAt", asset.uploadedAt().toString()
        );
    }

    private static Map<String, Object> toRightMap(ContentRight right) {
        return Map.of(
                "rightType", right.type().name(),
                "durationMonths", right.durationMonths() != null ? right.durationMonths() : 0
        );
    }
}
