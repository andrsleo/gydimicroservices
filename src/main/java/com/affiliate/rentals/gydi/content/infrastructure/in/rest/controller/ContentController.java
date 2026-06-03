package com.affiliate.rentals.gydi.content.infrastructure.in.rest.controller;

import com.affiliate.rentals.gydi.content.application.dto.*;
import com.affiliate.rentals.gydi.content.application.usecase.*;
import com.affiliate.rentals.gydi.content.domain.model.MediaType;
import com.affiliate.rentals.gydi.content.domain.model.ProcessingStatus;
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
import org.springframework.web.multipart.MultipartFile;

import com.affiliate.rentals.gydi.content.application.port.ContentMediaRepositoryPort;
import com.affiliate.rentals.gydi.content.application.port.ContentPostRepositoryPort;
import com.affiliate.rentals.gydi.content.domain.model.ContentMedia;
import com.affiliate.rentals.gydi.content.domain.model.ContentPost;
import com.affiliate.rentals.gydi.shared.domain.port.StoragePort;

@RestController
@RequestMapping("/api/v1/content")
@RequiredArgsConstructor
public class ContentController {

    private final CreateContentPostUseCase createContentPostUseCase;
    private final ArchiveContentPostUseCase archiveContentPostUseCase;
    private final UpdateContentPostUseCase updateContentPostUseCase;
    private final GetContentByIdUseCase getContentByIdUseCase;
    private final GetContentFeedUseCase getContentFeedUseCase;
    private final GetFollowingFeedUseCase getFollowingFeedUseCase;
    private final RegisterContentViewUseCase registerContentViewUseCase;
    private final GetCreatorContentUseCase getCreatorContentUseCase;
    private final GetPropertyContentUseCase getPropertyContentUseCase;
    private final ReportContentUseCase reportContentUseCase;
    private final ContentMediaRepositoryPort contentMediaRepositoryPort;
    private final ContentPostRepositoryPort contentPostRepositoryPort;
    private final StoragePort storagePort;
    private final OwnershipValidator ownershipValidator;
    // Phase 4 — Social Commerce
    private final GetContentBookingStatsUseCase getContentBookingStatsUseCase;

    @GetMapping("/feed")
    public ResponseEntity<ContentFeedPageDto> getFeed(
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(getContentFeedUseCase.execute(cursor, limit));
    }

    @GetMapping("/feed/following")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ContentFeedPageDto> getFollowingFeed(
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "10") int limit) {
        Long userId = ownershipValidator.getAuthenticatedUserId();
        return ResponseEntity.ok(getFollowingFeedUseCase.execute(userId, cursor, limit));
    }

    @PostMapping("/{id}/view")
    public ResponseEntity<Void> registerView(
            @PathVariable Long id,
            @RequestParam(required = false) String ip) {
        Long viewerId = null;
        try {
            viewerId = ownershipValidator.getAuthenticatedUserId();
        } catch (Exception ignored) {
            // anonymous view — viewerId stays null
        }
        registerContentViewUseCase.execute(id, viewerId, ip);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<ContentPostDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(getContentByIdUseCase.execute(id));
    }

    @GetMapping("/property/{propertyId}")
    public ResponseEntity<Page<ContentPostDto>> getByProperty(
            @PathVariable Long propertyId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {
        return ResponseEntity.ok(getPropertyContentUseCase.execute(propertyId,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "publishedAt"))));
    }

    @GetMapping("/my-content")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<ContentPostDto>> getMyContent(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {
        Long userId = ownershipValidator.getAuthenticatedUserId();
        return ResponseEntity.ok(getCreatorContentUseCase.execute(userId,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))));
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ContentPostDto> create(@Valid @RequestBody CreateContentPostRequest request) {
        Long userId = ownershipValidator.getAuthenticatedUserId();
        return ResponseEntity.status(HttpStatus.CREATED).body(createContentPostUseCase.execute(userId, request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ContentPostDto> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateContentPostRequest request) {
        Long userId = ownershipValidator.getAuthenticatedUserId();
        return ResponseEntity.ok(updateContentPostUseCase.execute(id, userId, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> archive(@PathVariable Long id) {
        Long userId = ownershipValidator.getAuthenticatedUserId();
        boolean isAdmin = ownershipValidator.isAdmin();
        archiveContentPostUseCase.execute(id, userId, isAdmin);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/report")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> report(
            @PathVariable Long id,
            @Valid @RequestBody ReportContentRequest request) {
        Long userId = ownershipValidator.getAuthenticatedUserId();
        reportContentUseCase.execute(id, userId, request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/media")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ContentMediaDto> uploadMedia(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file,
            @RequestParam(defaultValue = "0") int displayOrder) {
        ownershipValidator.getAuthenticatedUserId(); // verify authenticated

        String mediaTypeStr = file.getContentType() != null && file.getContentType().startsWith("video")
                ? "VIDEO" : "IMAGE";

        // Upload file to storage (local filesystem in dev, Cloudinary/S3 in prod)
        String storedUrl = storagePort.uploadFile(file, "content");
        boolean isImage = mediaTypeStr.equals("IMAGE");

        ContentMedia media = ContentMedia.builder()
                .contentPostId(id)
                .originalUrl(storedUrl)
                .thumbnailUrl(isImage ? storedUrl : null)
                .mediaType(MediaType.valueOf(mediaTypeStr))
                .displayOrder(displayOrder)
                .fileSizeBytes(file.getSize())
                .processingStatus(ProcessingStatus.READY)
                .build();

        ContentMedia saved = contentMediaRepositoryPort.save(media);

        // Update ContentPost.thumbnailUrl if not already set (first media uploaded)
        contentPostRepositoryPort.findById(id).ifPresent(post -> {
            if (post.thumbnailUrl() == null) {
                post.setThumbnailUrl(isImage ? storedUrl : null);
                contentPostRepositoryPort.save(post);
            }
        });

        return ResponseEntity.status(HttpStatus.CREATED).body(new ContentMediaDto(
                saved.id(), saved.contentPostId(), saved.originalUrl(), saved.processedUrl(),
                saved.thumbnailUrl(), saved.mediaType(), saved.displayOrder(), saved.durationSeconds(),
                saved.width(), saved.height(), saved.fileSizeBytes(), saved.processingStatus(), saved.createdAt()
        ));
    }

    @DeleteMapping("/{id}/media/{mediaId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deleteMedia(@PathVariable Long id, @PathVariable Long mediaId) {
        ownershipValidator.getAuthenticatedUserId();
        contentMediaRepositoryPort.deleteById(mediaId);
        return ResponseEntity.noContent().build();
    }

    // ─── Phase 4: Social Commerce ─────────────────────────────────────────────

    /**
     * GET /api/v1/content/{id}/booking-stats
     * Returns booking attribution stats for a content post.
     * Public endpoint — no authentication required.
     */
    @GetMapping("/{id}/booking-stats")
    public ResponseEntity<ContentBookingStatsDto> getContentBookingStats(@PathVariable Long id) {
        return ResponseEntity.ok(getContentBookingStatsUseCase.execute(id));
    }
}
