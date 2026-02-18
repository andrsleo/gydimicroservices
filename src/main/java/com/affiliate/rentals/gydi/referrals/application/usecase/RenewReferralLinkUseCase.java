package com.affiliate.rentals.gydi.referrals.application.usecase;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.affiliate.rentals.gydi.referrals.application.dto.RenewReferralLinkResponse;
import com.affiliate.rentals.gydi.referrals.domain.model.ReferralLink;
import com.affiliate.rentals.gydi.referrals.domain.port.ReferralLinkRepository;
import com.affiliate.rentals.gydi.shared.security.JwtReferralTokenService;
import com.affiliate.rentals.gydi.users.domain.model.SubscriptionPlan;
import com.affiliate.rentals.gydi.users.domain.model.User;
import com.affiliate.rentals.gydi.users.domain.ports.UserRepositoryPort;

import lombok.extern.slf4j.Slf4j;

/**
 * Caso de uso para renovar un enlace de referido existente
 *
 * FASE 2: Renovación manual de links
 *
 * Flujo:
 * 1. Validar que el link existe
 * 2. Validar que el link pertenece al usuario autenticado (seguridad)
 * 3. Obtener plan actual del usuario
 * 4. Calcular nueva fecha de expiración según plan ACTUAL
 * 5. Generar NUEVO token JWE con nueva expiración
 * 6. Actualizar link existente (NO crear nuevo registro):
 *    - Nuevo encrypted_token
 *    - Nueva expires_at
 *    - Status cambia de EXPIRED a ACTIVE si aplica
 *    - Mantiene mismo short_code
 * 7. Retornar respuesta con datos actualizados
 */
@Slf4j
@Service
@Transactional
public class RenewReferralLinkUseCase {

    private final ReferralLinkRepository referralLinkRepository;
    private final UserRepositoryPort userRepository;
    private final JwtReferralTokenService jwtReferralTokenService;
    private final String frontendUrl;

    public RenewReferralLinkUseCase(
            ReferralLinkRepository referralLinkRepository,
            UserRepositoryPort userRepository,
            JwtReferralTokenService jwtReferralTokenService,
            @org.springframework.beans.factory.annotation.Value("${app.frontend-url}") String frontendUrl) {
        this.referralLinkRepository = referralLinkRepository;
        this.userRepository = userRepository;
        this.jwtReferralTokenService = jwtReferralTokenService;
        this.frontendUrl = frontendUrl;
    }

    /**
     * Ejecuta el caso de uso de renovación
     *
     * @param linkId ID del enlace a renovar
     * @param authenticatedUserId ID del usuario autenticado (extraído del JWT)
     * @return respuesta con datos del link renovado
     * @throws IllegalArgumentException si el link no existe
     * @throws IllegalStateException si el usuario no es el owner del link
     */
    public RenewReferralLinkResponse execute(Long linkId, Long authenticatedUserId) {
        log.info("Renewing referral link ID: {} for user: {}", linkId, authenticatedUserId);

        // 1. Validar que el link existe
        ReferralLink link = referralLinkRepository.findById(linkId)
                .orElseThrow(() -> new IllegalArgumentException("Referral link not found: " + linkId));

        // 2. SECURITY: Validar que el link pertenece al usuario autenticado
        if (!link.getAffiliateId().equals(authenticatedUserId)) {
            log.warn("Security violation: User {} attempted to renew link {} owned by user {}",
                    authenticatedUserId, linkId, link.getAffiliateId());
            throw new IllegalStateException(
                    "Unauthorized: Cannot renew a link that doesn't belong to you");
        }

        // 3. Obtener usuario para determinar plan ACTUAL
        User user = userRepository.findById(authenticatedUserId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + authenticatedUserId));

        // 4. Calcular nueva expiración según plan ACTUAL del usuario
        int expirationDays = calculateExpirationDays(user.activePlan());
        LocalDateTime newExpiresAt = LocalDateTime.now().plusDays(expirationDays);
        long expirationMs = expirationDays * 24L * 60 * 60 * 1000;

        log.debug("User {} has plan {}, renewing link for {} days (expires: {})",
                authenticatedUserId, user.activePlan(), expirationDays, newExpiresAt);

        // 5. Generar NUEVO token JWE con nueva expiración
        String newEncryptedToken = jwtReferralTokenService.generateReferralToken(
                link.getAffiliateId(),
                link.getPropertyId(),
                expirationMs);

        // 6. Renovar link (método de dominio que actualiza token y expiración)
        link.renew(newEncryptedToken, newExpiresAt);

        // 7. Persistir cambios
        ReferralLink renewedLink = referralLinkRepository.update(link);

        log.info("Referral link renewed successfully. ID: {}, NewExpiresAt: {}, Plan: {}",
                renewedLink.getId(), renewedLink.getExpiresAt(), user.activePlan());

        // 8. Construir respuesta
        String fullUrl = buildFullUrl(renewedLink.getEncryptedToken());

        return new RenewReferralLinkResponse(
                renewedLink.getId(),
                renewedLink.getEncryptedToken(),
                renewedLink.getShortCode(),
                fullUrl,
                buildQrCodeUrl(fullUrl),
                renewedLink.getExpiresAt(),
                renewedLink.getUpdatedAt());
    }

    /**
     * Calcula los días de expiración basado en el plan de suscripción
     *
     * @param plan plan de suscripción del usuario
     * @return días de expiración (FREE: 30, PRO: 90, ELITE: 365)
     */
    private int calculateExpirationDays(SubscriptionPlan plan) {
        return switch (plan) {
            case FREE -> 30;
            case PRO -> 90;
            case ELITE -> 365;
        };
    }

    /**
     * Construye la URL completa del enlace usando el token JWE
     */
    private String buildFullUrl(String token) {
        return frontendUrl + "/ref?r=" + token;
    }

    /**
     * Construye la URL para generar el código QR
     */
    private String buildQrCodeUrl(String fullUrl) {
        return "https://api.qrserver.com/v1/create-qr-code/?size=300x300&data=" +
                java.net.URLEncoder.encode(fullUrl, java.nio.charset.StandardCharsets.UTF_8);
    }
}
