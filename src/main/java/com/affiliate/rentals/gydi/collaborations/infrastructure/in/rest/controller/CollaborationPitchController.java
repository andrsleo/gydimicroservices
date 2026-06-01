package com.affiliate.rentals.gydi.collaborations.infrastructure.in.rest.controller;

import com.affiliate.rentals.gydi.collaborations.application.usecase.*;
import com.affiliate.rentals.gydi.collaborations.application.usecase.command.*;
import com.affiliate.rentals.gydi.collaborations.domain.model.CounterOffer;
import com.affiliate.rentals.gydi.collaborations.domain.model.CreatorPitch;
import com.affiliate.rentals.gydi.collaborations.domain.model.enums.PitchStatus;
import com.affiliate.rentals.gydi.collaborations.infrastructure.in.rest.dto.*;
import com.affiliate.rentals.gydi.shared.security.OwnershipValidator;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * REST controller for creator pitch lifecycle.
 *
 * <p>Covers: create pitch, view pitch detail, host inbox, creator pitches,
 * accept / decline / cancel / counter-offer operations.
 *
 * @author GYDI Development Team
 */
@RestController
@RequestMapping("/api/v1/collaborations")
@RequiredArgsConstructor
public class CollaborationPitchController {

    private final CreatePitchUseCase createPitchUseCase;
    private final GetPitchDetailUseCase getPitchDetailUseCase;
    private final AcceptPitchUseCase acceptPitchUseCase;
    private final DeclinePitchUseCase declinePitchUseCase;
    private final CancelPitchUseCase cancelPitchUseCase;
    private final CreateCounterOfferUseCase createCounterOfferUseCase;
    private final GetHostInboxUseCase getHostInboxUseCase;
    private final GetCreatorPitchesUseCase getCreatorPitchesUseCase;
    private final OwnershipValidator ownershipValidator;

    // ── Pitch CRUD ────────────────────────────────────────────────────────────

    /**
     * POST /api/v1/collaborations/pitches
     * <p>Submit a new pitch. Requesting user must be a verified creator.
     */
    @PostMapping("/pitches")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> createPitch(
            @Valid @RequestBody CreatePitchRequest request) {
        Long creatorId = ownershipValidator.getAuthenticatedUserId();

        CreatePitchCommand command = new CreatePitchCommand(
                request.propertyId(),
                creatorId,
                request.introduction(),
                request.portfolioUrl(),
                request.preferredCheckIn(),
                request.preferredCheckOut(),
                request.deliverables(),
                request.compensation()
        );

        CreatorPitch pitch = createPitchUseCase.execute(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(toPitchDetail(pitch));
    }

    /**
     * GET /api/v1/collaborations/pitches/{pitchId}
     * <p>Get pitch details. Only the host or the creator may access.
     */
    @GetMapping("/pitches/{pitchId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> getPitch(@PathVariable Long pitchId) {
        Long userId = ownershipValidator.getAuthenticatedUserId();
        CreatorPitch pitch = getPitchDetailUseCase.execute(pitchId, userId);
        return ResponseEntity.ok(toPitchDetail(pitch));
    }

    // ── Pitch Actions ─────────────────────────────────────────────────────────

    /**
     * POST /api/v1/collaborations/pitches/{pitchId}/accept
     */
    @PostMapping("/pitches/{pitchId}/accept")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AcceptPitchResult> acceptPitch(@PathVariable Long pitchId) {
        Long userId = ownershipValidator.getAuthenticatedUserId();
        AcceptPitchResult result = acceptPitchUseCase.execute(new AcceptPitchCommand(pitchId, userId));
        return ResponseEntity.ok(result);
    }

    /**
     * POST /api/v1/collaborations/pitches/{pitchId}/decline
     */
    @PostMapping("/pitches/{pitchId}/decline")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DeclinePitchResult> declinePitch(
            @PathVariable Long pitchId,
            @RequestBody(required = false) DeclinePitchRequest request) {
        Long userId = ownershipValidator.getAuthenticatedUserId();
        String reason = request != null ? request.reason() : null;
        DeclinePitchResult result = declinePitchUseCase.execute(
                new DeclinePitchCommand(pitchId, userId, reason));
        return ResponseEntity.ok(result);
    }

    /**
     * POST /api/v1/collaborations/pitches/{pitchId}/cancel
     * <p>Only the creator may cancel a pitch.
     */
    @PostMapping("/pitches/{pitchId}/cancel")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> cancelPitch(@PathVariable Long pitchId) {
        Long userId = ownershipValidator.getAuthenticatedUserId();
        cancelPitchUseCase.execute(pitchId, userId);
        return ResponseEntity.noContent().build();
    }

    /**
     * POST /api/v1/collaborations/pitches/{pitchId}/counter-offers
     */
    @PostMapping("/pitches/{pitchId}/counter-offers")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CounterOfferResult> createCounterOffer(
            @PathVariable Long pitchId,
            @Valid @RequestBody CreateCounterOfferRequest request) {
        Long userId = ownershipValidator.getAuthenticatedUserId();

        CreateCounterOfferCommand command = new CreateCounterOfferCommand(
                pitchId,
                userId,
                request.offeredBy(),
                request.message(),
                request.compensationType(),
                request.nights(),
                request.amount(),
                request.currency(),
                request.bookingCommissionPct(),
                request.experienceItems(),
                request.deliverables()
        );

        CounterOfferResult result = createCounterOfferUseCase.execute(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    // ── Listing endpoints ─────────────────────────────────────────────────────

    /**
     * GET /api/v1/collaborations/inbox
     * <p>Host's pitch inbox with optional status/property filters.
     */
    @GetMapping("/inbox")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> getHostInbox(
            @RequestParam(required = false) PitchStatus status,
            @RequestParam(required = false) Long propertyId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Long hostId = ownershipValidator.getAuthenticatedUserId();

        Page<CreatorPitch> result = getHostInboxUseCase.execute(
                hostId, status, propertyId,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));

        return ResponseEntity.ok(toPageResponse(result));
    }

    /**
     * GET /api/v1/collaborations/my-pitches
     * <p>Creator's submitted pitches with optional status filter.
     */
    @GetMapping("/my-pitches")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> getMyPitches(
            @RequestParam(required = false) PitchStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Long creatorId = ownershipValidator.getAuthenticatedUserId();

        Page<CreatorPitch> result = getCreatorPitchesUseCase.execute(
                creatorId, status,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));

        return ResponseEntity.ok(toPageResponse(result));
    }

    // ── Mappers ───────────────────────────────────────────────────────────────

    private static Map<String, Object> toPitchDetail(CreatorPitch p) {
        List<Map<String, Object>> counterOffers = p.counterOffers().stream()
                .map(CollaborationPitchController::toCounterOfferMap)
                .collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("id", p.id());
        result.put("propertyId", p.propertyId());
        result.put("hostId", p.hostId());
        result.put("creatorId", p.creatorId());
        result.put("status", p.status().name());
        result.put("introduction", p.introduction());
        result.put("portfolioUrl", p.portfolioUrl() != null ? p.portfolioUrl() : "");
        result.put("preferredCheckIn", p.preferredCheckIn().toString());
        result.put("preferredCheckOut", p.preferredCheckOut().toString());
        result.put("counterOffers", counterOffers);
        result.put("expiresAt", p.expiresAt() != null ? p.expiresAt().toString() : null);
        result.put("createdAt", p.createdAt().toString());
        return result;
    }

    private static Map<String, Object> toCounterOfferMap(CounterOffer co) {
        return Map.of(
                "id", co.id(),
                "roundNumber", co.roundNumber(),
                "offeredBy", co.offeredBy().name(),
                "message", co.message() != null ? co.message() : "",
                "compensationType", co.compensationType().name(),
                "createdAt", co.createdAt().toString()
        );
    }

    private static Map<String, Object> toPitchSummary(CreatorPitch p) {
        return Map.of(
                "id", p.id(),
                "propertyId", p.propertyId(),
                "status", p.status().name(),
                "introduction", p.introduction().substring(0, Math.min(100, p.introduction().length())),
                "preferredCheckIn", p.preferredCheckIn().toString(),
                "preferredCheckOut", p.preferredCheckOut().toString(),
                "createdAt", p.createdAt().toString()
        );
    }

    private static Map<String, Object> toPageResponse(Page<CreatorPitch> page) {
        return Map.of(
                "content", page.getContent().stream()
                        .map(CollaborationPitchController::toPitchSummary)
                        .collect(Collectors.toList()),
                "totalElements", page.getTotalElements(),
                "totalPages", page.getTotalPages(),
                "currentPage", page.getNumber(),
                "pageSize", page.getSize()
        );
    }
}
