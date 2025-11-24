package com.affiliate.rentals.gydi.referrals.infrastructure.in.rest;

import com.affiliate.rentals.gydi.properties.domain.model.Property;
import com.affiliate.rentals.gydi.properties.domain.ports.out.PropertyRepositoryPort;
import com.affiliate.rentals.gydi.referrals.application.dto.*;
import com.affiliate.rentals.gydi.referrals.application.usecase.*;
import com.affiliate.rentals.gydi.referrals.domain.model.ReferralLink;
import com.affiliate.rentals.gydi.referrals.domain.port.ReferralLinkRepository;
import com.affiliate.rentals.gydi.shared.security.JwtReferralTokenService;
import com.affiliate.rentals.gydi.shared.security.JwtService;
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
 * - GET /api/v1/referrals/links - Listar enlaces del afiliado
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
        private final TrackClickUseCase trackClickUseCase;
        private final GetReferralStatsUseCase getReferralStatsUseCase;
        private final GetEarningsUseCase getEarningsUseCase;
        private final ReferralLinkRepository referralLinkRepository;
        private final JwtReferralTokenService jwtReferralTokenService;
        private final PropertyRepositoryPort propertyRepository;
        private final JwtService jwtService;
        private final String frontendUrl;

        public ReferralController(
                        GenerateReferralLinkUseCase generateReferralLinkUseCase,
                        TrackClickUseCase trackClickUseCase,
                        GetReferralStatsUseCase getReferralStatsUseCase,
                        GetEarningsUseCase getEarningsUseCase,
                        ReferralLinkRepository referralLinkRepository,
                        JwtReferralTokenService jwtReferralTokenService,
                        PropertyRepositoryPort propertyRepository,
                        JwtService jwtService,
                        @org.springframework.beans.factory.annotation.Value("${app.frontend-url}") String frontendUrl) {
                this.generateReferralLinkUseCase = generateReferralLinkUseCase;
                this.trackClickUseCase = trackClickUseCase;
                this.getReferralStatsUseCase = getReferralStatsUseCase;
                this.getEarningsUseCase = getEarningsUseCase;
                this.referralLinkRepository = referralLinkRepository;
                this.jwtReferralTokenService = jwtReferralTokenService;
                this.propertyRepository = propertyRepository;
                this.jwtService = jwtService;
                this.frontendUrl = frontendUrl;
        }

        /**
         * Generar un nuevo enlace de referido
         */
        @PostMapping("/links")
        @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
        @Operation(summary = "Generar enlace de referido", description = "Crea un nuevo enlace de referido para una propiedad específica. El afiliado puede compartir este enlace para ganar comisiones.", security = @SecurityRequirement(name = "bearerAuth"))
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "201", description = "Enlace creado exitosamente", content = @Content(schema = @Schema(implementation = GenerateReferralLinkResponse.class))),
                        @ApiResponse(responseCode = "400", description = "Datos inválidos o enlace ya existe"),
                        @ApiResponse(responseCode = "401", description = "No autenticado"),
                        @ApiResponse(responseCode = "403", description = "No tiene permisos de afiliado")
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
         * Listar enlaces de referido del afiliado
         */
        @GetMapping("/links")
        @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
        @Operation(summary = "Listar mis enlaces de referido", description = "Obtiene todos los enlaces de referido creados por el afiliado autenticado", security = @SecurityRequirement(name = "bearerAuth"))
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
                                link.getAffiliateId()));
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
         * Obtener estadísticas de referidos del afiliado
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
         * Obtener ganancias del afiliado
         */
        @GetMapping("/earnings")
        @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
        @Operation(summary = "Obtener ganancias del afiliado", description = "Obtiene el resumen de ganancias: total, pendiente, aprobado, pagado, y próximo pago", security = @SecurityRequirement(name = "bearerAuth"))
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
                                link.getConversionsCount(),
                                link.getTotalCommission(),
                                link.getConversionRate(),
                                link.getStatus(),
                                link.getExpiresAt(),
                                link.getCreatedAt(),
                                link.getUpdatedAt());
        }
}
