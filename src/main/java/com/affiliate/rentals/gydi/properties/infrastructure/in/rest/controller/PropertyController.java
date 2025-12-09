package com.affiliate.rentals.gydi.properties.infrastructure.in.rest.controller;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.affiliate.rentals.gydi.properties.application.dto.CreatePropertyRequest;
import com.affiliate.rentals.gydi.properties.application.dto.PropertyDetailResponse;
import com.affiliate.rentals.gydi.properties.application.dto.PropertyResponse;
import com.affiliate.rentals.gydi.properties.application.dto.ReorderImagesRequest;
import com.affiliate.rentals.gydi.properties.application.dto.ReorderImagesResponse;
import com.affiliate.rentals.gydi.properties.application.dto.UpdatePropertyRequest;
import com.affiliate.rentals.gydi.properties.application.mapper.PropertyMapper;
import com.affiliate.rentals.gydi.properties.domain.model.Property;
import com.affiliate.rentals.gydi.properties.domain.model.PropertyId;
import com.affiliate.rentals.gydi.properties.domain.model.PropertyStatus;
import com.affiliate.rentals.gydi.properties.domain.model.PropertyType;
import com.affiliate.rentals.gydi.properties.application.usecase.ReorderPropertyImagesUseCase;
import com.affiliate.rentals.gydi.properties.application.usecase.DeletePropertyImageUseCase;
import com.affiliate.rentals.gydi.properties.application.usecase.DeletePropertyVideoUseCase;
import com.affiliate.rentals.gydi.properties.domain.ports.in.ActivatePropertyUseCase;
import com.affiliate.rentals.gydi.properties.domain.ports.in.CreatePropertyUseCase;
import com.affiliate.rentals.gydi.properties.domain.ports.in.DeactivatePropertyUseCase;
import com.affiliate.rentals.gydi.properties.domain.ports.in.DeletePropertyUseCase;
import com.affiliate.rentals.gydi.properties.domain.ports.in.GetPropertyByIdUseCase;
import com.affiliate.rentals.gydi.properties.domain.ports.in.ListPropertiesUseCase;
import com.affiliate.rentals.gydi.properties.domain.ports.in.PublishPropertyUseCase;
import com.affiliate.rentals.gydi.properties.domain.ports.in.UpdatePropertyUseCase;
import com.affiliate.rentals.gydi.properties.domain.ports.out.PropertyRepositoryPort;
import com.affiliate.rentals.gydi.shared.security.JwtService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/properties")
public class PropertyController {

        private final CreatePropertyUseCase createPropertyUseCase;
        private final UpdatePropertyUseCase updatePropertyUseCase;
        private final GetPropertyByIdUseCase getPropertyByIdUseCase;
        private final ListPropertiesUseCase listPropertiesUseCase;
        private final PublishPropertyUseCase publishPropertyUseCase;
        private final ActivatePropertyUseCase activatePropertyUseCase;
        private final DeactivatePropertyUseCase deactivatePropertyUseCase;
        private final DeletePropertyUseCase deletePropertyUseCase;
        private final ReorderPropertyImagesUseCase reorderPropertyImagesUseCase;
        private final DeletePropertyImageUseCase deletePropertyImageUseCase;
        private final DeletePropertyVideoUseCase deletePropertyVideoUseCase;
        private final PropertyRepositoryPort propertyRepository;
        private final PropertyMapper mapper;
        private final JwtService jwtService;
        private final com.affiliate.rentals.gydi.properties.application.service.ICalUrlValidator iCalUrlValidator;
        private final com.affiliate.rentals.gydi.properties.domain.ports.in.GetPropertyCalendarBlocksUseCase getPropertyCalendarBlocksUseCase;

        public PropertyController(CreatePropertyUseCase createPropertyUseCase,
                        UpdatePropertyUseCase updatePropertyUseCase,
                        GetPropertyByIdUseCase getPropertyByIdUseCase,
                        ListPropertiesUseCase listPropertiesUseCase,
                        PublishPropertyUseCase publishPropertyUseCase,
                        ActivatePropertyUseCase activatePropertyUseCase,
                        DeactivatePropertyUseCase deactivatePropertyUseCase,
                        DeletePropertyUseCase deletePropertyUseCase,
                        ReorderPropertyImagesUseCase reorderPropertyImagesUseCase,
                        DeletePropertyImageUseCase deletePropertyImageUseCase,
                        DeletePropertyVideoUseCase deletePropertyVideoUseCase,
                        PropertyRepositoryPort propertyRepository,
                        PropertyMapper mapper,
                        JwtService jwtService,
                        com.affiliate.rentals.gydi.properties.application.service.ICalUrlValidator iCalUrlValidator,
                        com.affiliate.rentals.gydi.properties.domain.ports.in.GetPropertyCalendarBlocksUseCase getPropertyCalendarBlocksUseCase) {
                this.createPropertyUseCase = createPropertyUseCase;
                this.updatePropertyUseCase = updatePropertyUseCase;
                this.getPropertyByIdUseCase = getPropertyByIdUseCase;
                this.listPropertiesUseCase = listPropertiesUseCase;
                this.publishPropertyUseCase = publishPropertyUseCase;
                this.activatePropertyUseCase = activatePropertyUseCase;
                this.deactivatePropertyUseCase = deactivatePropertyUseCase;
                this.deletePropertyUseCase = deletePropertyUseCase;
                this.reorderPropertyImagesUseCase = reorderPropertyImagesUseCase;
                this.deletePropertyImageUseCase = deletePropertyImageUseCase;
                this.deletePropertyVideoUseCase = deletePropertyVideoUseCase;
                this.propertyRepository = propertyRepository;
                this.mapper = mapper;
                this.jwtService = jwtService;
                this.iCalUrlValidator = iCalUrlValidator;
                this.getPropertyCalendarBlocksUseCase = getPropertyCalendarBlocksUseCase;
        }

        @PostMapping
        public ResponseEntity<PropertyResponse> createProperty(
                        @Valid @RequestBody CreatePropertyRequest request,
                        HttpServletRequest httpRequest) {

                Long userId = jwtService.extractUserIdFromRequest(httpRequest);

                CreatePropertyUseCase.CreatePropertyCommand command = new CreatePropertyUseCase.CreatePropertyCommand(
                                userId,
                                request.title(),
                                request.description(),
                                request.pricePerNight(),
                                request.currency(),
                                request.salePrice(),
                                request.country(),
                                request.city(),
                                request.address(),
                                request.postalCode(),
                                request.amenities(),
                                request.bedrooms(),
                                request.bathrooms(),
                                request.maxGuests(),
                                request.propertyType(),
                                request.listingType(),
                                request.airbnbUrl(),
                                request.icalUrlAirbnb());

                Property property = createPropertyUseCase.createProperty(command);
                return ResponseEntity.status(HttpStatus.CREATED)
                                .body(mapper.toPropertyResponse(property));
        }

        @GetMapping("/{id}")
        @org.springframework.transaction.annotation.Transactional(readOnly = true)
        public ResponseEntity<PropertyDetailResponse> getProperty(@PathVariable String id) {
                return getPropertyByIdUseCase.getProperty(
                                new GetPropertyByIdUseCase.GetPropertyQuery(PropertyId.of(id)))
                                .map(mapper::toPropertyDetailResponse)
                                .map(ResponseEntity::ok)
                                .orElse(ResponseEntity.notFound().build());
        }

        /**
         * Get property by SEO-friendly slug.
         * Supports optional referral token for tracking.
         *
         * @param slug the property slug (e.g., "beach-house-malibu-x7k2m")
         * @param ref  optional JWT referral token
         * @return property details
         */
        @GetMapping("/by-slug/{slug}")
        @org.springframework.transaction.annotation.Transactional(readOnly = true)
        public ResponseEntity<PropertyDetailResponse> getPropertyBySlug(
                        @PathVariable String slug,
                        @RequestParam(required = false) String ref) {

                // TODO: Process referral token if present (track click, validate token)
                // This will be implemented when updating ReferralController

                return propertyRepository.findBySlug(slug)
                                .map(mapper::toPropertyDetailResponse)
                                .map(ResponseEntity::ok)
                                .orElse(ResponseEntity.notFound().build());
        }

        @GetMapping
        public ResponseEntity<PropertyPageResponse> listProperties(
                        @RequestParam(required = false) String status,
                        @RequestParam(required = false) String propertyType,
                        @RequestParam(required = false) String listingType,
                        @RequestParam(required = false) String country,
                        @RequestParam(required = false) String city,
                        @RequestParam(required = false) BigDecimal minPrice,
                        @RequestParam(required = false) BigDecimal maxPrice,
                        @RequestParam(required = false) Integer minBedrooms,
                        @RequestParam(required = false) Integer minBathrooms,
                        @RequestParam(required = false) Integer minGuests,
                        @RequestParam(required = false) List<String> amenities,
                        @RequestParam(required = false) String searchText,
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "20") int size,
                        @RequestParam(defaultValue = "createdAt") String sortBy,
                        @RequestParam(defaultValue = "DESC") String sortDirection) {

                ListPropertiesUseCase.ListPropertiesQuery query = new ListPropertiesUseCase.ListPropertiesQuery(
                                status != null ? PropertyStatus.valueOf(status) : null,
                                propertyType != null ? PropertyType.valueOf(propertyType) : null,
                                listingType,
                                country, city, minPrice, maxPrice, minBedrooms, minBathrooms, minGuests,
                                amenities, searchText,
                                page, size, sortBy, sortDirection);

                ListPropertiesUseCase.PropertyPage result = listPropertiesUseCase.listProperties(query);
                List<PropertyResponse> properties = mapper.toPropertyResponseList(result.properties());

                PropertyPageResponse response = new PropertyPageResponse(
                                properties, // content
                                result.totalElements(), // totalElements
                                result.totalPages(), // totalPages
                                result.currentPage(), // page
                                result.pageSize() // size
                );

                return ResponseEntity.ok(response);
        }

        @Transactional(readOnly = true)
        @GetMapping("/my-properties")
        public ResponseEntity<PropertyPageResponse> listMyProperties(
                        @RequestParam(required = false) String status,
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "20") int size,
                        HttpServletRequest httpRequest) {

                // Extract userId from JWT
                Long userId = jwtService.extractUserIdFromRequest(httpRequest);

                // Fetch properties by hostId
                List<Property> allUserProperties = propertyRepository.findByHostId(userId);

                // Apply status filter if provided
                List<Property> filteredProperties = allUserProperties;
                if (status != null && !status.isBlank()) {
                        PropertyStatus propertyStatus = PropertyStatus.valueOf(status);
                        filteredProperties = allUserProperties.stream()
                                        .filter(property -> property.getStatus() == propertyStatus)
                                        .toList();
                }

                // Calculate pagination
                long totalElements = filteredProperties.size();
                int totalPages = (int) Math.ceil((double) totalElements / size);
                int skip = page * size;

                // Apply manual pagination (skip/limit)
                List<Property> paginatedProperties = filteredProperties.stream()
                                .skip(skip)
                                .limit(size)
                                .toList();

                // Map to response
                List<PropertyResponse> properties = mapper.toPropertyResponseList(paginatedProperties);

                PropertyPageResponse response = new PropertyPageResponse(
                                properties, // content
                                totalElements, // totalElements
                                totalPages, // totalPages
                                page, // page
                                size // size
                );

                return ResponseEntity.ok(response);
        }

        @PutMapping("/{id}")
        public ResponseEntity<PropertyResponse> updateProperty(
                        @PathVariable String id,
                        @Valid @RequestBody UpdatePropertyRequest request,
                        HttpServletRequest httpRequest) {

                Long userId = jwtService.extractUserIdFromRequest(httpRequest);

                UpdatePropertyUseCase.UpdatePropertyCommand command = new UpdatePropertyUseCase.UpdatePropertyCommand(
                                PropertyId.of(id),
                                userId,
                                request.title(),
                                request.description(),
                                request.pricePerNight(),
                                request.currency(),
                                request.salePrice(),
                                request.country(),
                                request.city(),
                                request.address(),
                                request.postalCode(),
                                request.amenities(),
                                request.bedrooms(),
                                request.bathrooms(),
                                request.maxGuests(),
                                request.propertyType(),
                                request.listingType(),
                                request.airbnbUrl(),
                                request.icalUrlAirbnb());

                Property property = updatePropertyUseCase.updateProperty(command);
                return ResponseEntity.ok(mapper.toPropertyResponse(property));
        }

        @PostMapping("/{id}/publish")
        public ResponseEntity<PropertyResponse> publishProperty(
                        @PathVariable String id,
                        HttpServletRequest httpRequest) {

                Long userId = jwtService.extractUserIdFromRequest(httpRequest);

                Property property = publishPropertyUseCase.publishProperty(
                                new PublishPropertyUseCase.PublishPropertyCommand(PropertyId.of(id), userId));

                return ResponseEntity.ok(mapper.toPropertyResponse(property));
        }

        @PostMapping("/{id}/activate")
        public ResponseEntity<PropertyResponse> activateProperty(
                        @PathVariable String id,
                        HttpServletRequest httpRequest) {

                Long userId = jwtService.extractUserIdFromRequest(httpRequest);

                Property property = activatePropertyUseCase.activateProperty(
                                new ActivatePropertyUseCase.ActivatePropertyCommand(PropertyId.of(id), userId));

                return ResponseEntity.ok(mapper.toPropertyResponse(property));
        }

        @PostMapping("/{id}/deactivate")
        public ResponseEntity<PropertyResponse> deactivateProperty(
                        @PathVariable String id,
                        HttpServletRequest httpRequest) {

                Long userId = jwtService.extractUserIdFromRequest(httpRequest);

                Property property = deactivatePropertyUseCase.deactivateProperty(
                                new DeactivatePropertyUseCase.DeactivatePropertyCommand(PropertyId.of(id), userId));

                return ResponseEntity.ok(mapper.toPropertyResponse(property));
        }

        @DeleteMapping("/{id}")
        public ResponseEntity<Void> deleteProperty(
                        @PathVariable String id,
                        HttpServletRequest httpRequest) {

                Long userId = jwtService.extractUserIdFromRequest(httpRequest);

                deletePropertyUseCase.deleteProperty(
                                new DeletePropertyUseCase.DeletePropertyCommand(PropertyId.of(id), userId));

                return ResponseEntity.noContent().build();
        }

        @org.springframework.web.bind.annotation.PatchMapping("/{id}/images/reorder")
        public ResponseEntity<ReorderImagesResponse> reorderImages(
                        @PathVariable String id,
                        @Valid @RequestBody ReorderImagesRequest request,
                        HttpServletRequest httpRequest) {

                Long userId = jwtService.extractUserIdFromRequest(httpRequest);

                ReorderImagesResponse response = reorderPropertyImagesUseCase.execute(
                                Long.parseLong(id),
                                userId,
                                request);

                return ResponseEntity.ok(response);
        }

        @DeleteMapping("/{id}/images/{imageId}")
        public ResponseEntity<Void> deletePropertyImage(
                        @PathVariable String id,
                        @PathVariable String imageId,
                        HttpServletRequest httpRequest) {

                Long userId = jwtService.extractUserIdFromRequest(httpRequest);

                deletePropertyImageUseCase.execute(
                                Long.parseLong(id),
                                Long.parseLong(imageId),
                                userId);

                return ResponseEntity.noContent().build();
        }

        @DeleteMapping("/{id}/videos/{videoId}")
        public ResponseEntity<Void> deletePropertyVideo(
                        @PathVariable String id,
                        @PathVariable String videoId,
                        HttpServletRequest httpRequest) {

                Long userId = jwtService.extractUserIdFromRequest(httpRequest);

                deletePropertyVideoUseCase.execute(
                                Long.parseLong(id),
                                Long.parseLong(videoId),
                                userId);

                return ResponseEntity.noContent().build();
        }

        /**
         * Get calendar blocks for a property.
         * Returns blocked date ranges from external calendars (Airbnb, etc.)
         */
        @GetMapping("/{id}/calendar-blocks")
        @Transactional(readOnly = true)
        public ResponseEntity<List<com.affiliate.rentals.gydi.properties.application.dto.PropertyCalendarBlockResponse>> getCalendarBlocks(
                        @PathVariable String id) {

                List<com.affiliate.rentals.gydi.properties.domain.model.PropertyCalendarBlock> blocks = getPropertyCalendarBlocksUseCase
                                .getBlocks(Long.parseLong(id));

                List<com.affiliate.rentals.gydi.properties.application.dto.PropertyCalendarBlockResponse> response = blocks
                                .stream()
                                .map(block -> new com.affiliate.rentals.gydi.properties.application.dto.PropertyCalendarBlockResponse(
                                                block.getStartDate(),
                                                block.getEndDate(),
                                                block.getSource()))
                                .toList();

                return ResponseEntity.ok(response);
        }

        /**
         * Validate iCal URL.
         * Endpoint to validate iCal URLs before property creation/update.
         */
        @PostMapping("/validate-ical")
        public ResponseEntity<com.affiliate.rentals.gydi.properties.application.dto.ValidateICalUrlResponse> validateICalUrl(
                        @Valid @RequestBody com.affiliate.rentals.gydi.properties.application.dto.ValidateICalUrlRequest request) {

                com.affiliate.rentals.gydi.properties.application.service.ICalUrlValidator.ICalValidationResult result = iCalUrlValidator
                                .validateWithFetch(request.icalUrl());

                return ResponseEntity.ok(
                                com.affiliate.rentals.gydi.properties.application.dto.ValidateICalUrlResponse.of(
                                                result.isValid(),
                                                result.getMessage()));
        }

        record PropertyPageResponse(
                        List<PropertyResponse> content,
                        long totalElements,
                        int totalPages,
                        int page,
                        int size) {
        }
}
