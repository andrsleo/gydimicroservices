package com.affiliate.rentals.gydi.referrals.application.usecase;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.affiliate.rentals.gydi.referrals.application.dto.CommissionDto;
import com.affiliate.rentals.gydi.referrals.application.dto.EarningsDto;
import com.affiliate.rentals.gydi.referrals.domain.model.Commission;
import com.affiliate.rentals.gydi.referrals.domain.model.CommissionStatus;
import com.affiliate.rentals.gydi.referrals.domain.port.CommissionRepository;

import lombok.extern.slf4j.Slf4j;


/**
 * Caso de uso para obtener ganancias de un afiliado
 */
@Slf4j
@Service
@Transactional(readOnly = true)
public class GetEarningsUseCase {

    private static final BigDecimal MINIMUM_PAYOUT = new BigDecimal("50.00");

    private final CommissionRepository commissionRepository;

    public GetEarningsUseCase(CommissionRepository commissionRepository) {
        this.commissionRepository = commissionRepository;
    }

    /**
     * Obtiene el resumen de ganancias para un afiliado
     *
     * @param affiliateId ID del afiliado
     * @param currentPlan Plan actual del afiliado (FREE, PRO, ELITE)
     * @return EarningsDto con resumen completo
     */
    public EarningsDto execute(Long affiliateId, String currentPlan) {
        log.debug("Getting earnings for affiliate: {}", affiliateId);

        // Calcular totales por estado
        BigDecimal totalEarnings = commissionRepository.calculateTotalEarnings(affiliateId);
        BigDecimal pendingAmount = commissionRepository.calculateEarningsByStatus(
            affiliateId, CommissionStatus.PENDING);
        BigDecimal approvedAmount = commissionRepository.calculateEarningsByStatus(
            affiliateId, CommissionStatus.APPROVED);
        BigDecimal paidAmount = commissionRepository.calculateEarningsByStatus(
            affiliateId, CommissionStatus.PAID);

        // Contar comisiones por estado
        long pendingCount = commissionRepository.countByAffiliateIdAndStatus(
            affiliateId, CommissionStatus.PENDING);
        long approvedCount = commissionRepository.countByAffiliateIdAndStatus(
            affiliateId, CommissionStatus.APPROVED);
        long paidCount = commissionRepository.countByAffiliateIdAndStatus(
            affiliateId, CommissionStatus.PAID);

        // Próximo pago (si tiene comisiones aprobadas >= $50)
        BigDecimal nextPayoutAmount = approvedAmount;
        LocalDateTime nextPayoutDate = null;

        if (approvedAmount.compareTo(MINIMUM_PAYOUT) >= 0) {
            // Fecha de pago: próximo lunes (ejemplo)
            nextPayoutDate = calculateNextPayoutDate();
        }

        // Historial reciente (últimas 10 comisiones)
        List<Commission> recentCommissions = commissionRepository
            .findByAffiliateId(affiliateId)
            .stream()
            .sorted((c1, c2) -> c2.getCreatedAt().compareTo(c1.getCreatedAt()))
            .limit(10)
            .toList();

        // Convertir a DTOs
        List<CommissionDto> recentCommissionDtos = recentCommissions.stream()
            .map(this::mapToDto)
            .toList();

        // Determinar tasa de comisión actual
        BigDecimal currentCommissionRate = getCommissionRateForPlan(currentPlan);

        return new EarningsDto(
            affiliateId,
            currentPlan,
            currentCommissionRate,
            totalEarnings,
            pendingAmount,
            approvedAmount,
            paidAmount,
            (int) pendingCount,
            (int) approvedCount,
            (int) paidCount,
            nextPayoutAmount,
            nextPayoutDate,
            recentCommissionDtos
        );
    }

    /**
     * Convierte Commission a CommissionDto
     */
    private CommissionDto mapToDto(Commission commission) {
        return new CommissionDto(
            commission.getId(),
            commission.getReferralLinkId(),
            commission.getAffiliateId(),
            commission.getPropertyId(),
            commission.getCommissionRate(),
            commission.getCommissionAmount(),
            commission.getAffiliatePlan(),
            commission.getStatus(),
            commission.getRemainingHoldDays(),
            commission.isReadyForApproval(),
            commission.isPayable(),
            commission.getCreatedAt()
        );
    }

    /**
     * Calcula la próxima fecha de pago (próximo lunes)
     */
    private LocalDateTime calculateNextPayoutDate() {
        LocalDateTime now = LocalDateTime.now();
        int daysUntilMonday = (8 - now.getDayOfWeek().getValue()) % 7;
        if (daysUntilMonday == 0) {
            daysUntilMonday = 7; // Si hoy es lunes, el próximo pago es en 7 días
        }
        return now.plusDays(daysUntilMonday).withHour(10).withMinute(0).withSecond(0);
    }

    /**
     * Obtiene la tasa de comisión para un plan
     */
    private BigDecimal getCommissionRateForPlan(String plan) {
        return switch (plan.toUpperCase()) {
            case "FREE", "BASIC" -> new BigDecimal("0.02");   // 2%
            case "PRO" -> new BigDecimal("0.05");             // 5%
            case "ELITE", "PLUS" -> new BigDecimal("0.15");   // 15%
            default -> BigDecimal.ZERO;
        };
    }
}