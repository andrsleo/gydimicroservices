package com.affiliate.rentals.gydi.referrals.application.usecase;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.affiliate.rentals.gydi.referrals.application.dto.TrackClickRequest;
import com.affiliate.rentals.gydi.referrals.domain.model.DeviceType;
import com.affiliate.rentals.gydi.referrals.domain.model.ReferralClick;
import com.affiliate.rentals.gydi.referrals.domain.model.ReferralLink;
import com.affiliate.rentals.gydi.referrals.domain.port.ReferralClickRepository;
import com.affiliate.rentals.gydi.referrals.domain.port.ReferralLinkRepository;

import lombok.extern.slf4j.Slf4j;

/**
 * Caso de uso para rastrear clicks en enlaces de referido
 *
 * Flujo:
 * 1. Validar que el enlace existe y está activo
 * 2. Hash de datos PII (IP, User-Agent) para GDPR
 * 3. Detectar tipo de dispositivo
 * 4. Calcular score de bot
 * 5. Detectar clicks duplicados
 * 6. Crear registro de click
 * 7. Incrementar contador en el enlace
 */
@Slf4j
@Service
@Transactional
public class TrackClickUseCase {

    private static final String HASH_SALT = "gydi-referral-2025"; // En producción: usar secret externo
    private final ReferralLinkRepository referralLinkRepository;
    private final ReferralClickRepository referralClickRepository;

    public TrackClickUseCase(ReferralLinkRepository referralLinkRepository,
                             ReferralClickRepository referralClickRepository) {
        this.referralLinkRepository = referralLinkRepository;
        this.referralClickRepository = referralClickRepository;
    }

    /**
     * Ejecuta el caso de uso
     */
    public void execute(TrackClickRequest request) {
        log.debug("Tracking click for referral link: {}", request.referralLinkId());

        // 1. Obtener y validar enlace
        ReferralLink referralLink = referralLinkRepository.findById(request.referralLinkId())
            .orElseThrow(() -> new IllegalArgumentException("Referral link not found"));

        if (!referralLink.isActive()) {
            log.warn("Attempted to track click on inactive link: {}", referralLink.getId());
            throw new IllegalStateException("Referral link is not active");
        }

        // 2. Hash de PII para GDPR compliance
        byte[] ipHash = hashPII(request.ipAddress());
        byte[] userAgentHash = hashPII(request.userAgent());

        // 3. Detectar tipo de dispositivo
        DeviceType deviceType = detectDeviceType(request.userAgent());

        // 4. Calcular score de bot
        Integer botScore = calculateBotScore(request.userAgent(), request.fingerprint());

        // 5. Verificar clicks duplicados (misma IP + UA en últimos 5 minutos)
        LocalDateTime recentThreshold = LocalDateTime.now().minusMinutes(5);
        List<ReferralClick> recentDuplicates = referralClickRepository.findDuplicateClicks(
            request.referralLinkId(),
            ipHash,
            userAgentHash,
            recentThreshold
        );

        if (!recentDuplicates.isEmpty()) {
            log.debug("Duplicate click detected (same IP+UA in last 5 min), not counting");
            return; // No contar click duplicado
        }

        // 6. Crear registro de click
        ReferralClick click = ReferralClick.create(
            request.referralLinkId(),
            ipHash,
            userAgentHash,
            request.fingerprint(),
            request.countryCode(),
            deviceType,
            botScore
        );

        referralClickRepository.save(click);

        // 7. Incrementar contador en enlace (solo si no es probable bot)
        if (botScore == null || botScore < 70) {
            referralLink.incrementClicks();
            referralLinkRepository.update(referralLink);

            log.info("Click tracked successfully. Link: {}, Device: {}, BotScore: {}",
                referralLink.getId(), deviceType, botScore);
        } else {
            log.warn("Probable bot detected. Link: {}, BotScore: {}", referralLink.getId(), botScore);
        }
    }

    /**
     * Hash SHA-256 de datos PII para cumplir GDPR
     */
    private byte[] hashPII(String data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String saltedData = data + HASH_SALT;
            return digest.digest(saltedData.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    /**
     * Detecta el tipo de dispositivo basado en User-Agent
     */
    private DeviceType detectDeviceType(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) {
            return DeviceType.UNKNOWN;
        }

        String ua = userAgent.toLowerCase();

        // Tablets primero (muchos tablets se identifican como mobile)
        if (ua.contains("tablet") || ua.contains("ipad")) {
            return DeviceType.TABLET;
        }

        // Mobile
        if (ua.contains("mobile") || ua.contains("android") ||
            ua.contains("iphone") || ua.contains("ipod") ||
            ua.contains("windows phone") || ua.contains("blackberry")) {
            return DeviceType.MOBILE;
        }

        // Desktop (por defecto si no es mobile/tablet)
        if (ua.contains("windows") || ua.contains("macintosh") ||
            ua.contains("linux") || ua.contains("x11")) {
            return DeviceType.DESKTOP;
        }

        return DeviceType.UNKNOWN;
    }

    /**
     * Calcula un score de probabilidad de bot (0-100)
     * Mayor score = más probable que sea bot
     */
    private Integer calculateBotScore(String userAgent, String fingerprint) {
        if (userAgent == null || userAgent.isBlank()) {
            return 90; // Sin UA es muy sospechoso
        }

        int score = 0;
        String ua = userAgent.toLowerCase();

        // Bots conocidos
        if (ua.contains("bot") || ua.contains("crawler") || ua.contains("spider") ||
            ua.contains("scraper") || ua.contains("wget") || ua.contains("curl")) {
            score += 50;
        }

        // User-Agent sospechoso (muy corto o genérico)
        if (userAgent.length() < 20) {
            score += 30;
        }

        // Sin fingerprint es sospechoso
        if (fingerprint == null || fingerprint.isBlank()) {
            score += 20;
        }

        // Headless browsers
        if (ua.contains("headless") || ua.contains("phantom")) {
            score += 40;
        }

        // Automatización
        if (ua.contains("selenium") || ua.contains("puppeteer") || ua.contains("playwright")) {
            score += 50;
        }

        return Math.min(score, 100); // Máximo 100
    }
}