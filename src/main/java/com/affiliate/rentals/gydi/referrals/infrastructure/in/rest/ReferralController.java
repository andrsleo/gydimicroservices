package com.affiliate.rentals.gydi.referrals.infrastructure.in.rest;

import com.affiliate.rentals.gydi.properties.domain.model.Property;
import com.affiliate.rentals.gydi.properties.domain.ports.out.PropertyRepositoryPort;
import com.affiliate.rentals.gydi.referrals.application.dto.*;
import com.affiliate.rentals.gydi.referrals.application.usecase.*;
import com.affiliate.rentals.gydi.referrals.domain.model.ReferralLink;
import com.affiliate.rentals.gydi.referrals.domain.port.ReferralLinkRepository;
import com.affiliate.rentals.gydi.shared.security.JwtReferralTokenService;
import com.affiliate.rentals.gydi.shared.security.JwtService;
import com.affiliate.rentals.gydi.users.infrastructure.out.persistence.entity.UserEntity;
import com.affiliate.rentals.gydi.users.infrastructure.out.persistence.repository.UserJpaRepository;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * REST Controller para el sistema de referidos
 *
 * Endpoints:
 * - POST /api/v1/referrals/links - Generar enlace de referido
 * - GET /api/v1/referrals/links - Listar enlaces del referido
 * - GET /api/v1/referrals/links/{id} - Obtener enlace por ID
 * - POST /api/v1/referrals/clicks - Registrar click (público)
 * - GET /api/v1/referrals/stats - Obtener estadísticas
 * - GET /api/v1/referrals/earnings - Obtener ganancias
 */
@RestController
@RequestMapping("/api/v1/referrals")
@Slf4j
@Tag(name = "Referrals", description = "Sistema de referidos y comisiones")
public class ReferralController {

        private final GenerateReferralLinkUseCase generateReferralLinkUseCase;
        private final RenewReferralLinkUseCase renewReferralLinkUseCase;
        private final TrackClickUseCase trackClickUseCase;
        private final GetReferralStatsUseCase getReferralStatsUseCase;
        private final GetEarningsUseCase getEarningsUseCase;
        private final ReferralLinkRepository referralLinkRepository;
        private final JwtReferralTokenService jwtReferralTokenService;
        private final PropertyRepositoryPort propertyRepository;
        private final JwtService jwtService;
        private final UserJpaRepository userJpaRepository;
        private final String frontendUrl;

        public ReferralController(
                        GenerateReferralLinkUseCase generateReferralLinkUseCase,
                        RenewReferralLinkUseCase renewReferralLinkUseCase,
                        TrackClickUseCase trackClickUseCase,
                        GetReferralStatsUseCase getReferralStatsUseCase,
                        GetEarningsUseCase getEarningsUseCase,
                        ReferralLinkRepository referralLinkRepository,
                        JwtReferralTokenService jwtReferralTokenService,
                        PropertyRepositoryPort propertyRepository,
                        JwtService jwtService,
                        UserJpaRepository userJpaRepository,
                        @org.springframework.beans.factory.annotation.Value("${app.frontend-url}") String frontendUrl) {
                this.generateReferralLinkUseCase = generateReferralLinkUseCase;
                this.renewReferralLinkUseCase = renewReferralLinkUseCase;
                this.trackClickUseCase = trackClickUseCase;
                this.getReferralStatsUseCase = getReferralStatsUseCase;
                this.getEarningsUseCase = getEarningsUseCase;
                this.referralLinkRepository = referralLinkRepository;
                this.jwtReferralTokenService = jwtReferralTokenService;
                this.propertyRepository = propertyRepository;
                this.jwtService = jwtService;
                this.userJpaRepository = userJpaRepository;
                this.frontendUrl = frontendUrl;
        }

        /**
         * Generar un nuevo enlace de referido
         */
        @PostMapping("/links")
        @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
        @Operation(summary = "Generar enlace de referido", description = "Crea un nuevo enlace de referido para una propiedad específica. El referido puede compartir este enlace para ganar comisiones.", security = @SecurityRequirement(name = "bearerAuth"))
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "201", description = "Enlace creado exitosamente", content = @Content(schema = @Schema(implementation = GenerateReferralLinkResponse.class))),
                        @ApiResponse(responseCode = "400", description = "Datos inválidos o enlace ya existe"),
                        @ApiResponse(responseCode = "401", description = "No autenticado"),
                        @ApiResponse(responseCode = "403", description = "No tiene permisos de referido")
        })
        public ResponseEntity<GenerateReferralLinkResponse> generateReferralLink(
                        @Valid @RequestBody GenerateReferralLinkRequest request,
                        HttpServletRequest httpRequest) {

                // SECURITY: Extract user ID from JWT token (server-side, trusted source)
                // Client cannot manipulate this value - prevents IDOR attacks
                Long authenticatedUserId = jwtService.extractUserIdFromRequest(httpRequest);

                log.info("Generating referral link for affiliate: {}, property: {}",
                                authenticatedUserId, request.propertyId());

                // Pass affiliateId as separate parameter from controller (trusted source)
                GenerateReferralLinkResponse response = generateReferralLinkUseCase.execute(
                                authenticatedUserId,
                                request);

                return ResponseEntity.status(HttpStatus.CREATED).body(response);
        }

        /**
         * Listar enlaces de referido del referido
         */
        @GetMapping("/links")
        @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
        @Transactional(readOnly = true) // Fix LazyInitializationException when loading Property images
        @Operation(summary = "Listar mis enlaces de referido", description = "Obtiene todos los enlaces de referido creados por el referido autenticado", security = @SecurityRequirement(name = "bearerAuth"))
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Lista de enlaces obtenida exitosamente"),
                        @ApiResponse(responseCode = "401", description = "No autenticado")
        })
        public ResponseEntity<List<ReferralLinkDto>> getMyReferralLinks(HttpServletRequest httpRequest) {
                Long userId = jwtService.extractUserIdFromRequest(httpRequest);
                log.debug("Fetching referral links for user: {}", userId);

                List<ReferralLink> links = referralLinkRepository.findByAffiliateId(userId);
                List<ReferralLinkDto> dtos = links.stream()
                                .map(this::mapToDto)
                                .collect(Collectors.toList());

                return ResponseEntity.ok(dtos);
        }

        /**
         * Obtener enlace de referido por ID
         */
        @GetMapping("/links/{id}")
        @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
        @Transactional(readOnly = true) // Fix LazyInitializationException when loading Property images
        @Operation(summary = "Obtener enlace por ID", description = "Obtiene los detalles de un enlace de referido específico", security = @SecurityRequirement(name = "bearerAuth"))
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Enlace encontrado", content = @Content(schema = @Schema(implementation = ReferralLinkDto.class))),
                        @ApiResponse(responseCode = "404", description = "Enlace no encontrado"),
                        @ApiResponse(responseCode = "403", description = "No tiene acceso a este enlace")
        })
        public ResponseEntity<ReferralLinkDto> getReferralLinkById(
                        @PathVariable Long id,
                        HttpServletRequest httpRequest,
                        Authentication authentication) {

                Long userId = jwtService.extractUserIdFromRequest(httpRequest);

                ReferralLink link = referralLinkRepository.findById(id)
                                .orElseThrow(() -> new IllegalArgumentException("Referral link not found"));

                // Verificar que el enlace pertenece al usuario (excepto ADMIN)
                if (!hasRole(authentication, "ADMIN") && !link.getAffiliateId().equals(userId)) {
                        log.warn("User {} attempted to access link owned by affiliate {}",
                                        userId, link.getAffiliateId());
                        return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
                }

                return ResponseEntity.ok(mapToDto(link));
        }

        /**
         * Renovar un enlace de referido existente
         *
         * FASE 2: Renovación manual de links
         * - Genera nuevo token con expiración basada en plan ACTUAL del usuario
         * - Actualiza el mismo registro (NO crea nuevo link)
         * - Reactiva links expirados
         * - Mantiene mismo short_code
         */
        @PutMapping("/links/{id}/renew")
        @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
        @Operation(summary = "Renovar enlace de referido", description = "Renueva un enlace existente con nueva expiración basada en el plan actual del usuario. "
                        +
                        "Reactiva enlaces expirados y genera nuevo token JWE. Mantiene el mismo código corto.", security = @SecurityRequirement(name = "bearerAuth"))
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Enlace renovado exitosamente", content = @Content(schema = @Schema(implementation = RenewReferralLinkResponse.class))),
                        @ApiResponse(responseCode = "400", description = "Datos inválidos o enlace no puede ser renovado"),
                        @ApiResponse(responseCode = "401", description = "No autenticado"),
                        @ApiResponse(responseCode = "403", description = "No tiene permisos para renovar este enlace"),
                        @ApiResponse(responseCode = "404", description = "Enlace no encontrado")
        })
        public ResponseEntity<RenewReferralLinkResponse> renewReferralLink(
                        @PathVariable Long id,
                        HttpServletRequest httpRequest) {

                // SECURITY: Extract user ID from JWT token (server-side, trusted source)
                Long userId = jwtService.extractUserIdFromRequest(httpRequest);

                log.info("Renewing referral link ID: {} for user: {}", id, userId);

                // Execute use case (validates ownership inside)
                RenewReferralLinkResponse response = renewReferralLinkUseCase.execute(id, userId);

                return ResponseEntity.ok(response);
        }

        /**
         * Resolver un token de referido y registrar el click
         */
        @PostMapping("/resolve")
        @Transactional
        @Operation(summary = "Resolver token de referido", description = "Valida un token, registra el click y devuelve la URL de destino.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Token válido", content = @Content(schema = @Schema(implementation = ResolveReferralResponse.class))),
                        @ApiResponse(responseCode = "400", description = "Token inválido o expirado"),
                        @ApiResponse(responseCode = "404", description = "Enlace no encontrado")
        })
        public ResponseEntity<ResolveReferralResponse> resolveReferralLink(
                        @Valid @RequestBody ResolveReferralRequest request) {
                log.debug("Resolving referral token");

                // 1. Validate and extract data from JWE token
                JwtReferralTokenService.ReferralTokenData tokenData = jwtReferralTokenService
                                .validateAndExtract(request.token());

                // 2. Find active link
                ReferralLink link = referralLinkRepository.findActiveLink(tokenData.userId(), tokenData.propertyId())
                                .orElseThrow(() -> new IllegalArgumentException(
                                                "Active referral link not found for token data"));

                // 3. Track click
                TrackClickRequest trackRequest = new TrackClickRequest(
                                link.getId(),
                                request.ipAddress() != null ? request.ipAddress() : "unknown",
                                request.userAgent() != null ? request.userAgent() : "unknown",
                                request.fingerprint(),
                                request.countryCode(),
                                request.referer());
                trackClickUseCase.execute(trackRequest);

                // 4. Get property slug for destination URL
                Property property = propertyRepository.findById(
                                com.affiliate.rentals.gydi.properties.domain.model.PropertyId.of(link.getPropertyId()))
                                .orElseThrow(() -> new IllegalArgumentException(
                                                "Property not found: " + link.getPropertyId()));

                String slug = property.getSlug();
                if (slug == null || slug.isBlank()) {
                        slug = link.getPropertyId().toString();
                }

                // Use configured frontend URL
                String destinationUrl = frontendUrl + "/propiedades/" + slug;

                return ResponseEntity.ok(new ResolveReferralResponse(
                                destinationUrl,
                                link.getPropertyId(),
                                link.getAffiliateId(),
                                link.getId()));
        }

        /**
         * Registrar un click en un enlace de referido (endpoint público)
         * 
         * @deprecated Use /resolve instead
         */
        @Deprecated
        @PostMapping("/clicks")
        @Operation(summary = "Registrar click en enlace", description = "Registra un click cuando alguien accede a través de un enlace de referido. Este endpoint es público y se llama desde el frontend.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "204", description = "Click registrado exitosamente"),
                        @ApiResponse(responseCode = "400", description = "Datos inválidos"),
                        @ApiResponse(responseCode = "404", description = "Enlace no encontrado o inactivo")
        })
        public ResponseEntity<Void> trackClick(@Valid @RequestBody TrackClickRequest request) {
                log.debug("Tracking click for referral link: {}", request.referralLinkId());

                trackClickUseCase.execute(request);
                return ResponseEntity.noContent().build();
        }

        /**
         * Obtener estadísticas de referidos del referido
         */
        @GetMapping("/stats")
        @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
        @Operation(summary = "Obtener estadísticas de referidos", description = "Obtiene estadísticas completas del sistema de referidos: clicks, conversiones, ganancias, etc.", security = @SecurityRequirement(name = "bearerAuth"))
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Estadísticas obtenidas exitosamente", content = @Content(schema = @Schema(implementation = ReferralStatsDto.class))),
                        @ApiResponse(responseCode = "401", description = "No autenticado")
        })
        public ResponseEntity<ReferralStatsDto> getStats(
                        @RequestParam(required = false) Long affiliateId,
                        HttpServletRequest httpRequest,
                        Authentication authentication) {

                Long userId = jwtService.extractUserIdFromRequest(httpRequest);

                // Si no se especifica affiliateId, usar el del usuario autenticado
                Long targetAffiliateId = affiliateId != null ? affiliateId : userId;

                // Solo ADMIN puede ver stats de otros usuarios
                if (!targetAffiliateId.equals(userId) && !hasRole(authentication, "ADMIN")) {
                        return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
                }

                ReferralStatsDto stats = getReferralStatsUseCase.execute(targetAffiliateId);
                return ResponseEntity.ok(stats);
        }

        /**
         * Obtener ganancias del referido
         */
        @GetMapping("/earnings")
        @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
        @Operation(summary = "Obtener ganancias del referido", description = "Obtiene el resumen de ganancias: total, pendiente, aprobado, pagado, y próximo pago", security = @SecurityRequirement(name = "bearerAuth"))
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Ganancias obtenidas exitosamente", content = @Content(schema = @Schema(implementation = EarningsDto.class))),
                        @ApiResponse(responseCode = "401", description = "No autenticado")
        })
        public ResponseEntity<EarningsDto> getEarnings(
                        @RequestParam(required = false) String currentPlan,
                        HttpServletRequest httpRequest) {

                Long userId = jwtService.extractUserIdFromRequest(httpRequest);

                // Si no se especifica plan, usar FREE por defecto
                // TODO: Obtener el plan real del usuario desde el servicio de subscripciones
                String plan = currentPlan != null ? currentPlan : "FREE";

                EarningsDto earnings = getEarningsUseCase.execute(userId, plan);
                return ResponseEntity.ok(earnings);
        }

        /**
         * Obtener el ID del link de referido del sistema (orgánico) para una propiedad
         * Este endpoint es público y se usa cuando un cliente llega sin link de
         * referido
         */
        @GetMapping("/public/system-link/{propertyId}")
        @Operation(summary = "Obtener link de referido del sistema", description = "Retorna el ID del link de referido genérico del sistema para tracking de tráfico orgánico (sin referido)")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Link del sistema encontrado o creado"),
                        @ApiResponse(responseCode = "404", description = "Propiedad no encontrada"),
                        @ApiResponse(responseCode = "500", description = "Error al crear link del sistema")
        })
        public ResponseEntity<SystemLinkResponse> getSystemReferralLink(@PathVariable Long propertyId) {
                log.info("GET /public/system-link/{} - Getting or creating system referral link", propertyId);

                try {
                        // 1. Find SYSTEM user
                        UserEntity systemUser = userJpaRepository.findByEmail("system-organic@gydi.internal")
                                        .orElseThrow(() -> new IllegalStateException(
                                                        "SYSTEM user not found. Please run migration V47__create_system_user_for_organic_traffic.sql"));

                        Long systemUserId = systemUser.getId();
                        log.debug("SYSTEM user found with ID: {}", systemUserId);

                        // 2. Find or create system link for this property
                        java.util.Optional<ReferralLink> existingLink = referralLinkRepository
                                        .findActiveLink(systemUserId, propertyId);

                        ReferralLink systemLink;
                        if (existingLink.isPresent()) {
                                systemLink = existingLink.get();
                                log.debug("System link already exists with ID: {}", systemLink.getId());
                        } else {
                                // Create new system link using GenerateReferralLinkUseCase
                                // Expiration days are now calculated automatically based on user's plan
                                GenerateReferralLinkRequest request = new GenerateReferralLinkRequest(propertyId);
                                GenerateReferralLinkResponse response = generateReferralLinkUseCase
                                                .execute(systemUserId, request);

                                // Fetch the created link
                                systemLink = referralLinkRepository.findById(response.id())
                                                .orElseThrow(() -> new IllegalStateException(
                                                                "Failed to create system link"));

                                log.info("Created new system link with ID: {} for property: {}", systemLink.getId(),
                                                propertyId);
                        }

                        return ResponseEntity.ok(new SystemLinkResponse(systemLink.getId()));

                } catch (IllegalStateException e) {
                        log.error("System configuration error: {}", e.getMessage());
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
                } catch (Exception e) {
                        log.error("Error getting system referral link for property {}: {}", propertyId, e.getMessage(),
                                        e);
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
                }
        }

        // Métodos auxiliares

        /**
         * Verifica si el usuario tiene un rol específico
         */
        private boolean hasRole(Authentication authentication, String role) {
                return authentication.getAuthorities().stream()
                                .anyMatch(a -> a.getAuthority().equals("ROLE_" + role));
        }

        /**
         * Mapea ReferralLink a ReferralLinkDto con SEO-friendly URL + JWT token
         */
        private ReferralLinkDto mapToDto(ReferralLink link) {
                // Get property to extract slug
                Property property = propertyRepository.findById(
                                com.affiliate.rentals.gydi.properties.domain.model.PropertyId.of(link.getPropertyId()))
                                .orElseThrow(() -> new IllegalArgumentException(
                                                "Property not found: " + link.getPropertyId()));

                // Get SEO-friendly slug
                String slug = property.getSlug();
                if (slug == null || slug.isBlank()) {
                        log.warn("Property {} has no slug, using ID as fallback", link.getPropertyId());
                        slug = link.getPropertyId().toString();
                }

                // Use existing encrypted token from the link (JWE)
                String referralToken = link.getEncryptedToken();

                // Build new URL format:
                // https://midominio.com/ref?r={JWE_TOKEN}
                String fullUrl = frontendUrl + "/ref?r=" + referralToken;

                return new ReferralLinkDto(
                                link.getId(),
                                link.getAffiliateId(),
                                link.getPropertyId(),
                                link.getShortCode(),
                                fullUrl,
                                link.getClicksCount(),
                                link.getStatus(),
                                link.getExpiresAt(),
                                link.getCreatedAt(),
                                link.getUpdatedAt());
        }

        /**
         * Response DTO for system referral link
         */
        public record SystemLinkResponse(Long referralLinkId) {
        }
}
