package com.affiliate.rentals.gydi.referrals.application.usecase;

import java.security.SecureRandom;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.affiliate.rentals.gydi.referrals.application.dto.GenerateReferralLinkRequest;
import com.affiliate.rentals.gydi.referrals.application.dto.GenerateReferralLinkResponse;
import com.affiliate.rentals.gydi.referrals.domain.model.ReferralLink;
import com.affiliate.rentals.gydi.referrals.domain.port.ReferralLinkRepository;
import com.affiliate.rentals.gydi.shared.security.JwtReferralTokenService;

import lombok.extern.slf4j.Slf4j;

/**
 * Caso de uso para generar un nuevo enlace de referido
 *
 * Flujo:
 * 1. Validar que el afiliado no tenga ya un enlace activo para esa propiedad
 * 2. Generar token JWE (Encrypted JWT)
 * 3. Generar código corto único (para uso interno/DB)
 * 4. Crear enlace de referido
 * 5. Persistir en base de datos
 * 6. Retornar URL completa con el token
 */

@Slf4j
@Service
@Transactional
public class GenerateReferralLinkUseCase {

    private static final String SHORT_CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"; // Sin 0, O, 1, I
    private static final int SHORT_CODE_LENGTH = 8;

    private final ReferralLinkRepository referralLinkRepository;
    private final JwtReferralTokenService jwtReferralTokenService;
    private final SecureRandom secureRandom;
    private final String frontendUrl;

    public GenerateReferralLinkUseCase(
            ReferralLinkRepository referralLinkRepository,
            JwtReferralTokenService jwtReferralTokenService,
            @org.springframework.beans.factory.annotation.Value("${app.frontend-url}") String frontendUrl) {
        this.referralLinkRepository = referralLinkRepository;
        this.jwtReferralTokenService = jwtReferralTokenService;
        this.secureRandom = new SecureRandom();
        this.frontendUrl = frontendUrl;
    }

    /**
     * Ejecuta el caso de uso
     *
     * @param affiliateId extracted from JWT token by controller (server-side,
     *                    trusted)
     * @param request     contains only propertyId and expirationDays (client input)
     */
    public GenerateReferralLinkResponse execute(Long affiliateId, GenerateReferralLinkRequest request) {
        log.info("Generating referral link for affiliate: {} and property: {}",
                affiliateId, request.propertyId());

        // 1. Check if an active link already exists
        java.util.Optional<ReferralLink> existingLink = referralLinkRepository.findActiveLink(
                affiliateId,
                request.propertyId());

        if (existingLink.isPresent()) {
            ReferralLink link = existingLink.get();
            log.info("Returning existing active referral link. ID: {}", link.getId());

            String fullUrl = buildFullUrl(link.getEncryptedToken());

            return new GenerateReferralLinkResponse(
                    link.getId(),
                    link.getEncryptedToken(),
                    link.getShortCode(),
                    fullUrl,
                    buildQrCodeUrl(fullUrl),
                    link.getExpiresAt(),
                    link.getCreatedAt());
        }

        // 2. Generar token encriptado (JWE)
        String encryptedToken = jwtReferralTokenService.generateReferralToken(
                affiliateId,
                request.propertyId());

        // 3. Generar código corto único (para persistencia y uso legacy si aplica)
        String shortCode = generateUniqueShortCode();

        // 4. Calcular fecha de expiración
        LocalDateTime expiresAt = LocalDateTime.now().plusDays(request.expirationDays());

        // 5. Crear enlace de referido
        ReferralLink referralLink = ReferralLink.create(
                affiliateId,
                request.propertyId(),
                encryptedToken,
                shortCode,
                expiresAt);

        // 6. Persistir
        ReferralLink savedLink = referralLinkRepository.save(referralLink);

        log.info("Referral link created successfully. ID: {}, ShortCode: {}",
                savedLink.getId(), savedLink.getShortCode());

        // 7. Construir respuesta
        String fullUrl = buildFullUrl(encryptedToken);

        return new GenerateReferralLinkResponse(
                savedLink.getId(),
                savedLink.getEncryptedToken(),
                savedLink.getShortCode(),
                fullUrl,
                buildQrCodeUrl(fullUrl),
                savedLink.getExpiresAt(),
                savedLink.getCreatedAt());
    }

    /**
     * Genera un código corto único (8 caracteres)
     */
    private String generateUniqueShortCode() {
        String shortCode;
        int attempts = 0;
        final int maxAttempts = 10;

        do {
            shortCode = generateRandomShortCode();
            attempts++;

            if (attempts >= maxAttempts) {
                throw new IllegalStateException(
                        "Could not generate unique short code after " + maxAttempts + " attempts");
            }
        } while (referralLinkRepository.findByShortCode(shortCode).isPresent());

        return shortCode;
    }

    /**
     * Genera un código corto aleatorio
     */
    private String generateRandomShortCode() {
        StringBuilder code = new StringBuilder(SHORT_CODE_LENGTH);

        for (int i = 0; i < SHORT_CODE_LENGTH; i++) {
            int randomIndex = secureRandom.nextInt(SHORT_CODE_CHARS.length());
            code.append(SHORT_CODE_CHARS.charAt(randomIndex));
        }

        return code.toString();
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
        // URL para QR code usando servicio externo (ejemplo: qrcode.show)
        return "https://api.qrserver.com/v1/create-qr-code/?size=300x300&data=" +
                java.net.URLEncoder.encode(fullUrl, java.nio.charset.StandardCharsets.UTF_8);
    }
}