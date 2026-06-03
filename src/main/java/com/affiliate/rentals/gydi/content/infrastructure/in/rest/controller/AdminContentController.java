package com.affiliate.rentals.gydi.content.infrastructure.in.rest.controller;

import com.affiliate.rentals.gydi.content.application.dto.ContentReportDto;
import com.affiliate.rentals.gydi.content.application.dto.ReviewReportRequest;
import com.affiliate.rentals.gydi.content.application.port.ContentPostRepositoryPort;
import com.affiliate.rentals.gydi.content.application.port.ContentReportRepositoryPort;
import com.affiliate.rentals.gydi.content.application.usecase.ReviewContentReportUseCase;
import com.affiliate.rentals.gydi.content.application.mapper.ContentDtoMapper;
import com.affiliate.rentals.gydi.content.domain.exception.ContentNotFoundException;
import com.affiliate.rentals.gydi.content.domain.model.ReportStatus;
import com.affiliate.rentals.gydi.shared.security.OwnershipValidator;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/content")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminContentController {

    private final ReviewContentReportUseCase reviewContentReportUseCase;
    private final ContentReportRepositoryPort contentReportRepository;
    private final ContentPostRepositoryPort contentPostRepository;
    private final ContentDtoMapper mapper;
    private final OwnershipValidator ownershipValidator;

    @GetMapping("/reports")
    public ResponseEntity<Page<ContentReportDto>> getPendingReports(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<ContentReportDto> reports = contentReportRepository
                .findByStatus(ReportStatus.PENDING, PageRequest.of(page, size))
                .map(mapper::toReportDto);
        return ResponseEntity.ok(reports);
    }

    @PutMapping("/reports/{reportId}")
    public ResponseEntity<Void> reviewReport(
            @PathVariable Long reportId,
            @Valid @RequestBody ReviewReportRequest request) {
        Long adminId = ownershipValidator.getAuthenticatedUserId();
        reviewContentReportUseCase.execute(reportId, adminId, request);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}/reject")
    public ResponseEntity<Void> rejectContent(@PathVariable Long id) {
        var post = contentPostRepository.findById(id)
                .orElseThrow(() -> new ContentNotFoundException("Contenido no encontrado: " + id));
        post.reject();
        contentPostRepository.save(post);
        return ResponseEntity.ok().build();
    }
}
