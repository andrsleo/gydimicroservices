package com.affiliate.rentals.gydi.content.infrastructure.in.rest.controller;

import com.affiliate.rentals.gydi.content.application.dto.ContentFeedPageDto;
import com.affiliate.rentals.gydi.content.application.dto.ContentPostDto;
import com.affiliate.rentals.gydi.content.application.usecase.GetCollaborativeRecommendationsUseCase;
import com.affiliate.rentals.gydi.content.application.usecase.GetRecommendedContentUseCase;
import com.affiliate.rentals.gydi.content.application.usecase.GetSimilarContentUseCase;
import com.affiliate.rentals.gydi.shared.security.OwnershipValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for content recommendation endpoints.
 *
 * <p>Phase 5 — Recommendation Engine (PostgreSQL-native, no external ML).</p>
 *
 * <ul>
 *   <li>{@code GET /api/v1/content/{id}/similar} — public, 10 similar posts</li>
 *   <li>{@code GET /api/v1/content/recommended} — authenticated, personalized feed</li>
 *   <li>{@code GET /api/v1/content/collaborative/{id}} — authenticated, collaborative filtering</li>
 * </ul>
 *
 * @author GYDI Development Team
 */
@RestController
@RequestMapping("/api/v1/content")
@RequiredArgsConstructor
public class RecommendationController {

    private final GetSimilarContentUseCase getSimilarContentUseCase;
    private final GetRecommendedContentUseCase getRecommendedContentUseCase;
    private final GetCollaborativeRecommendationsUseCase getCollaborativeRecommendationsUseCase;
    private final OwnershipValidator ownershipValidator;

    /**
     * GET /api/v1/content/{id}/similar
     * Returns up to 10 published posts similar to the given post.
     * Public endpoint — no authentication required.
     */
    @GetMapping("/{id}/similar")
    public ResponseEntity<List<ContentPostDto>> getSimilarContent(@PathVariable Long id) {
        return ResponseEntity.ok(getSimilarContentUseCase.execute(id));
    }

    /**
     * GET /api/v1/content/recommended
     * Returns a personalized feed for the authenticated user, based on their
     * likes and saves history. Supports optional limit parameter.
     */
    @GetMapping("/recommended")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ContentFeedPageDto> getRecommendedContent(
            @RequestParam(defaultValue = "20") int limit) {
        Long userId = ownershipValidator.getAuthenticatedUserId();
        return ResponseEntity.ok(getRecommendedContentUseCase.execute(userId, limit));
    }

    /**
     * GET /api/v1/content/collaborative/{id}
     * Returns collaborative filtering recommendations: "users who liked post X
     * also liked post Y". Requires authentication.
     */
    @GetMapping("/collaborative/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ContentPostDto>> getCollaborativeRecommendations(@PathVariable Long id) {
        Long userId = ownershipValidator.getAuthenticatedUserId();
        return ResponseEntity.ok(getCollaborativeRecommendationsUseCase.execute(id, userId));
    }
}
