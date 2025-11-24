package com.affiliate.rentals.gydi.referrals.application.usecase;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.affiliate.rentals.gydi.referrals.domain.model.Commission;
import com.affiliate.rentals.gydi.referrals.domain.model.ReferralLink;
import com.affiliate.rentals.gydi.referrals.domain.port.CommissionRepository;
import com.affiliate.rentals.gydi.referrals.domain.port.ReferralLinkRepository;

import lombok.extern.slf4j.Slf4j;

/**
 * Caso de uso para registrar una conversión (suscripción referida completada)
 *
 * Flujo:
 * 1. Validar que el enlace existe y está activo
 * 2. Obtener el plan del afiliado y su tasa de comisión
 * 3. Crear entrada en commission_ledger
 * 4. Generar hash de verificación
 * 5. Incrementar contador de conversiones en el enlace
 * 6. Actualizar total de comisión en el enlace
 */
@Slf4j
@Service
@Transactional
public class RegisterConversionUseCase {

    private final ReferralLinkRepository referralLinkRepository;
    private final CommissionRepository commissionRepository;

    public RegisterConversionUseCase(ReferralLinkRepository referralLinkRepository,
            CommissionRepository commissionRepository) {
        this.referralLinkRepository = referralLinkRepository;
        this.commissionRepository = commissionRepository;
    }

    /**
     * Registra una conversión cuando se completa una suscripción referida
     *
     * @param referralLinkId ID del enlace de referido
     * @param propertyId     ID de la propiedad (puede ser null para conversiones generales)
     * @param commissionAmount Monto de comisión a pagar (ya calculado externamente)
     * @param affiliatePlan  Plan del afiliado (FREE, PRO, ELITE)
     * @return Commission creada
     */
    public Commission execute(Long referralLinkId,
            Long propertyId,
            BigDecimal commissionAmount,
            String affiliatePlan) {

        log.info("Registering conversion. ReferralLink: {}, Property: {}, Commission: {}",
                referralLinkId, propertyId, commissionAmount);

        // 1. Validar enlace
        ReferralLink referralLink = referralLinkRepository.findById(referralLinkId)
                .orElseThrow(() -> new IllegalArgumentException("Referral link not found"));

        if (!referralLink.isActive()) {
            throw new IllegalStateException("Cannot register conversion on inactive link");
        }

        // 2. Determinar tasa de comisión según el plan
        BigDecimal commissionRate = getCommissionRate(affiliatePlan);

        // 3. Crear comisión
        Commission commission = Commission.create(
                referralLinkId,
                referralLink.getAffiliateId(),
                propertyId,
                commissionAmount,
                commissionRate,
                affiliatePlan);

        // 4. Generar hash de verificación
        byte[] verificationHash = generateVerificationHash(commission);
        commission.setVerificationHash(verificationHash);

        // 5. Persistir comisión
        Commission savedCommission = commissionRepository.save(commission);

        // 6. Actualizar enlace (incrementar conversiones y sumar comisión)
        referralLink.registerConversion(commission.getCommissionAmount());
        referralLinkRepository.update(referralLink);

        log.info("Conversion registered successfully. Commission: {}, Amount: {}",
                savedCommission.getId(), savedCommission.getCommissionAmount());

        return savedCommission;
    }

    /**
     * Obtiene la tasa de comisión según el plan del afiliado
     */
    private BigDecimal getCommissionRate(String affiliatePlan) {
        return switch (affiliatePlan.toUpperCase()) {
            case "FREE", "BASIC" -> new BigDecimal("0.02"); // 2%
            case "PRO" -> new BigDecimal("0.05"); // 5%
            case "ELITE", "PLUS" -> new BigDecimal("0.15"); // 15%
            default -> throw new IllegalArgumentException("Invalid affiliate plan: " + affiliatePlan);
        };
    }

    /**
     * Genera un hash SHA-256 de verificación para detectar manipulación
     * Hash incluye: affiliateId, propertyId, commissionAmount, commissionRate,
     * affiliatePlan, timestamp
     */
    private byte[] generateVerificationHash(Commission commission) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            String data = commission.getAffiliateId() + "|" +
                    commission.getPropertyId() + "|" +
                    commission.getCommissionAmount().toPlainString() + "|" +
                    commission.getCommissionRate().toPlainString() + "|" +
                    commission.getAffiliatePlan() + "|" +
                    commission.getCreatedAt().toString();

            return digest.digest(data.getBytes(StandardCharsets.UTF_8));

        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }
}