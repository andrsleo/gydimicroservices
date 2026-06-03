package com.affiliate.rentals.gydi.users.infrastructure.in.rest.controller;

import com.affiliate.rentals.gydi.shared.security.OwnershipValidator;
import com.affiliate.rentals.gydi.users.application.dto.CreatorProfileResponse;
import com.affiliate.rentals.gydi.users.application.usecase.UpgradeToCreatorUseCase;
import com.affiliate.rentals.gydi.users.domain.ports.CreatorProfileRepositoryPort;
import com.affiliate.rentals.gydi.users.domain.model.CreatorProfile;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class CreatorController {

    private final UpgradeToCreatorUseCase upgradeToCreatorUseCase;
    private final CreatorProfileRepositoryPort creatorProfileRepository;
    private final OwnershipValidator ownershipValidator;

    @PostMapping("/upgrade-to-creator")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, String>> upgradeToCreator() {
        Long userId = ownershipValidator.getAuthenticatedUserId();
        upgradeToCreatorUseCase.execute(userId);
        return ResponseEntity.ok(Map.of("message", "Cuenta de Creator activada exitosamente"));
    }

    @GetMapping("/{id}/creator-profile")
    public ResponseEntity<CreatorProfileResponse> getCreatorProfile(@PathVariable Long id) {
        return creatorProfileRepository.findByUserId(id)
                .map(this::toResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/creators")
    public ResponseEntity<Page<CreatorProfileResponse>> browseCreators(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {
        Page<CreatorProfileResponse> result = creatorProfileRepository
                .findAll(PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "totalViews")))
                .map(this::toResponse);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/creators/top")
    public ResponseEntity<List<CreatorProfileResponse>> getTopCreators(
            @RequestParam(defaultValue = "6") int limit) {
        List<CreatorProfileResponse> result = creatorProfileRepository.findTop(limit)
                .stream().map(this::toResponse).toList();
        return ResponseEntity.ok(result);
    }

    private CreatorProfileResponse toResponse(CreatorProfile p) {
        return new CreatorProfileResponse(p.userId(), p.contentCount(), p.followerCount(),
                p.followingCount(), p.totalViews(), p.avgEngagementRate(), p.tier(),
                p.lastContentAt(), p.updatedAt());
    }
}
