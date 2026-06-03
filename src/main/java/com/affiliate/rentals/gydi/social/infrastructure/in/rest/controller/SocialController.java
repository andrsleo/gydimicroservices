package com.affiliate.rentals.gydi.social.infrastructure.in.rest.controller;

import com.affiliate.rentals.gydi.shared.security.OwnershipValidator;
import com.affiliate.rentals.gydi.shared.security.RateLimitService;
import com.affiliate.rentals.gydi.social.application.dto.*;
import com.affiliate.rentals.gydi.social.application.usecase.*;
import com.affiliate.rentals.gydi.social.domain.model.SharePlatform;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/social")
@RequiredArgsConstructor
public class SocialController {

    private final ToggleFollowUseCase toggleFollowUseCase;
    private final ToggleLikeUseCase toggleLikeUseCase;
    private final ToggleSaveUseCase toggleSaveUseCase;
    private final AddCommentUseCase addCommentUseCase;
    private final GetCommentsUseCase getCommentsUseCase;
    private final DeleteCommentUseCase deleteCommentUseCase;
    private final UpdateCommentUseCase updateCommentUseCase;
    private final RegisterShareUseCase registerShareUseCase;
    private final GetFollowersUseCase getFollowersUseCase;
    private final GetFollowingUseCase getFollowingUseCase;
    private final GetSavedContentUseCase getSavedContentUseCase;
    private final OwnershipValidator ownershipValidator;
    private final RateLimitService rateLimitService;

    // ── Follow ────────────────────────────────────────────────────────────────

    @PostMapping("/follow/{userId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Boolean>> toggleFollow(
            @PathVariable Long userId,
            HttpServletRequest request) {
        if (!rateLimitService.tryConsumeFollow(request)) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Demasiadas solicitudes de follow. Inténtalo más tarde.");
        }
        Long me = ownershipValidator.getAuthenticatedUserId();
        boolean following = toggleFollowUseCase.execute(me, userId);
        return ResponseEntity.ok(Map.of("following", following));
    }

    @GetMapping("/followers/{userId}")
    public ResponseEntity<Page<FollowUserDto>> getFollowers(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(getFollowersUseCase.execute(userId,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))));
    }

    @GetMapping("/following/{userId}")
    public ResponseEntity<Page<FollowUserDto>> getFollowing(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(getFollowingUseCase.execute(userId,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))));
    }

    // ── Like ─────────────────────────────────────────────────────────────────

    @PostMapping("/like/{contentId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ToggleLikeResponse> toggleLike(
            @PathVariable Long contentId,
            HttpServletRequest request) {
        if (!rateLimitService.tryConsumeLike(request)) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Demasiados likes. Inténtalo más tarde.");
        }
        Long userId = ownershipValidator.getAuthenticatedUserId();
        return ResponseEntity.ok(toggleLikeUseCase.execute(userId, contentId));
    }

    // ── Save ─────────────────────────────────────────────────────────────────

    @PostMapping("/save/{contentId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ToggleSaveResponse> toggleSave(
            @PathVariable Long contentId,
            HttpServletRequest request) {
        Long userId = ownershipValidator.getAuthenticatedUserId();
        return ResponseEntity.ok(toggleSaveUseCase.execute(userId, contentId));
    }

    @GetMapping("/saves")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<SavedItemDto>> getSavedContent(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {
        Long userId = ownershipValidator.getAuthenticatedUserId();
        return ResponseEntity.ok(getSavedContentUseCase.execute(userId,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))));
    }

    // ── Comments ─────────────────────────────────────────────────────────────

    @GetMapping("/comments/{contentId}")
    public ResponseEntity<Page<CommentDto>> getComments(
            @PathVariable Long contentId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(getCommentsUseCase.execute(contentId,
                PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "createdAt"))));
    }

    @PostMapping("/comments/{contentId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CommentDto> addComment(
            @PathVariable Long contentId,
            @Valid @RequestBody AddCommentRequest request,
            HttpServletRequest httpRequest) {
        if (!rateLimitService.tryConsumeComment(httpRequest)) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Demasiados comentarios. Inténtalo más tarde.");
        }
        Long userId = ownershipValidator.getAuthenticatedUserId();
        return ResponseEntity.ok(addCommentUseCase.execute(contentId, userId, request));
    }

    @PutMapping("/comments/{commentId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CommentDto> updateComment(
            @PathVariable Long commentId,
            @Valid @RequestBody UpdateCommentRequest request) {
        Long userId = ownershipValidator.getAuthenticatedUserId();
        return ResponseEntity.ok(updateCommentUseCase.execute(commentId, userId, request));
    }

    @DeleteMapping("/comments/{commentId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deleteComment(@PathVariable Long commentId) {
        Long userId = ownershipValidator.getAuthenticatedUserId();
        boolean isAdmin = ownershipValidator.isAdmin();
        deleteCommentUseCase.execute(commentId, userId, isAdmin);
        return ResponseEntity.noContent().build();
    }

    // ── Share ─────────────────────────────────────────────────────────────────

    @PostMapping("/share/{contentId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> registerShare(
            @PathVariable Long contentId,
            @RequestParam(defaultValue = "OTHER") String platform) {
        Long userId = ownershipValidator.getAuthenticatedUserId();
        registerShareUseCase.execute(contentId, userId, SharePlatform.valueOf(platform));
        return ResponseEntity.ok().build();
    }
}
