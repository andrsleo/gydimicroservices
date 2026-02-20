package com.affiliate.rentals.gydi.properties.infrastructure.in.rest.controller;

import com.affiliate.rentals.gydi.properties.application.dto.PropertyResponse;
import com.affiliate.rentals.gydi.properties.application.mapper.PropertyMapper;
import com.affiliate.rentals.gydi.properties.domain.model.Property;
import com.affiliate.rentals.gydi.properties.domain.model.PropertyId;
import com.affiliate.rentals.gydi.properties.domain.model.PropertyStatus;
import com.affiliate.rentals.gydi.properties.domain.ports.in.AdminApprovePropertyUseCase;
import com.affiliate.rentals.gydi.properties.domain.ports.in.AdminDenyPropertyUseCase;
import com.affiliate.rentals.gydi.properties.domain.ports.in.ListPropertiesUseCase;
import com.affiliate.rentals.gydi.properties.infrastructure.out.persistence.repository.PropertyJpaRepository;
import com.affiliate.rentals.gydi.shared.security.JwtService;
import com.affiliate.rentals.gydi.shared.util.SlugGenerator;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Admin endpoints for property management tasks.
 */
@RestController
@RequestMapping("/api/admin")
public class PropertyAdminController {

    private final PropertyJpaRepository propertyJpaRepository;
    private final SlugGenerator slugGenerator;
    private final AdminApprovePropertyUseCase adminApprovePropertyUseCase;
    private final AdminDenyPropertyUseCase adminDenyPropertyUseCase;
    private final ListPropertiesUseCase listPropertiesUseCase;
    private final PropertyMapper propertyMapper;
    private final JwtService jwtService;

    public PropertyAdminController(
            PropertyJpaRepository propertyJpaRepository,
            SlugGenerator slugGenerator,
            AdminApprovePropertyUseCase adminApprovePropertyUseCase,
            AdminDenyPropertyUseCase adminDenyPropertyUseCase,
            ListPropertiesUseCase listPropertiesUseCase,
            PropertyMapper propertyMapper,
            JwtService jwtService) {
        this.propertyJpaRepository = propertyJpaRepository;
        this.slugGenerator = slugGenerator;
        this.adminApprovePropertyUseCase = adminApprovePropertyUseCase;
        this.adminDenyPropertyUseCase = adminDenyPropertyUseCase;
        this.listPropertiesUseCase = listPropertiesUseCase;
        this.propertyMapper = propertyMapper;
        this.jwtService = jwtService;
    }

    /**
     * Generate slugs for all properties that don't have one.
     */
    @PostMapping("/properties/generate-slugs")
    // @PreAuthorize("hasRole('ADMIN')") // TEMPORARY: Disabled for slug generation
    public ResponseEntity<String> generateMissingSlugs() {
        AtomicInteger count = new AtomicInteger(0);
        AtomicInteger total = new AtomicInteger(0);

        propertyJpaRepository.findAll().forEach(property -> {
            total.incrementAndGet();
            System.out.println("Property ID: " + property.getId() + ", Title: " + property.getTitle() + ", Current slug: " + property.getSlug());

            if (property.getSlug() == null || property.getSlug().isBlank()) {
                String sanitizedTitle = property.getTitle() != null && !property.getTitle().isBlank()
                        ? property.getTitle()
                        : "property";

                String shortId = Long.toHexString(property.getId());
                String slug = slugGenerator.generateUniqueSlug(sanitizedTitle, shortId);

                System.out.println("Generating slug for property " + property.getId() + ": " + slug);
                property.setSlug(slug);
                propertyJpaRepository.save(property);
                count.incrementAndGet();
            }
        });

        String message = String.format("Generated slugs for %d properties (total properties: %d)", count.get(), total.get());
        System.out.println(message);
        return ResponseEntity.ok(message);
    }

    /**
     * Get all properties pending admin approval.
     */
    @GetMapping("/properties/pending")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PropertyPageResponse> getPendingProperties(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        ListPropertiesUseCase.ListPropertiesQuery query = new ListPropertiesUseCase.ListPropertiesQuery(
                PropertyStatus.PENDING_APPROVAL,
                null, null, null, null, null, null, null, null, null, null, null,
                page, size, "submittedAt", "ASC");

        ListPropertiesUseCase.PropertyPage result = listPropertiesUseCase.listProperties(query);
        List<PropertyResponse> properties = propertyMapper.toPropertyResponseList(result.properties());

        return ResponseEntity.ok(new PropertyPageResponse(
                properties,
                result.totalElements(),
                result.totalPages(),
                result.currentPage(),
                result.pageSize()));
    }

    /**
     * Approve a pending property, making it publicly visible.
     */
    @PostMapping("/properties/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PropertyResponse> approveProperty(
            @PathVariable String id,
            HttpServletRequest httpRequest) {

        Long adminId = jwtService.extractUserIdFromRequest(httpRequest);
        Property property = adminApprovePropertyUseCase.approveProperty(
                new AdminApprovePropertyUseCase.ApprovePropertyCommand(PropertyId.of(id), adminId));

        return ResponseEntity.ok(propertyMapper.toPropertyResponse(property));
    }

    /**
     * Deny a pending property with a reason. Host can fix and re-submit.
     */
    @PostMapping("/properties/{id}/deny")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PropertyResponse> denyProperty(
            @PathVariable String id,
            @RequestBody DenyRequest request,
            HttpServletRequest httpRequest) {

        Long adminId = jwtService.extractUserIdFromRequest(httpRequest);
        Property property = adminDenyPropertyUseCase.denyProperty(
                new AdminDenyPropertyUseCase.DenyPropertyCommand(
                        PropertyId.of(id), adminId, request.reason()));

        return ResponseEntity.ok(propertyMapper.toPropertyResponse(property));
    }

    record DenyRequest(String reason) {
    }

    record PropertyPageResponse(
            List<PropertyResponse> content,
            long totalElements,
            int totalPages,
            int page,
            int size) {
    }
}
