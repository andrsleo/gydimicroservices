package com.affiliate.rentals.gydi.referrals.application.usecase;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Service;

import com.affiliate.rentals.gydi.commissions.domain.model.ReferralCommission;
import com.affiliate.rentals.gydi.commissions.domain.model.ReferralCommissionStatus;
import com.affiliate.rentals.gydi.commissions.domain.ports.ReferralCommissionRepositoryPort;
import com.affiliate.rentals.gydi.referrals.application.dto.CommissionDto;
import com.affiliate.rentals.gydi.referrals.application.dto.EarningsDto;

import lombok.extern.slf4j.Slf4j;

/**
 * Caso de uso para obtener ganancias de un referido.
 * 
 * Implementación actualizada para usar el nuevo módulo de comisiones
 * (commissions schema).
 * Reemplaza la antigua implementación basada en referrals.commission_booking.
 */
@Slf4j
@Service
public class GetEarningsUseCase {

    private final ReferralCommissionRepositoryPort commissionRepository;

    public GetEarningsUseCase(ReferralCommissionRepositoryPort commissionRepository) {
        this.commissionRepository = commissionRepository;
    }

    /**
     * Obtiene el resumen de ganancias para un referido usando el nuevo módulo de
     * comisiones.
     *
     * @param affiliateId ID del referido
     * @param currentPlan Plan actual del referido
     * @return EarningsDto con datos reales del nuevo sistema
     */
    public EarningsDto execute(Long affiliateId, String currentPlan) {
        log.debug("Getting earnings for affiliate: {} from commissions module", affiliateId);

        List<ReferralCommission> commissions = commissionRepository.findByAffiliateId(affiliateId);

        BigDecimal totalEarnings = commissions.stream()
                .map(c -> c.getAmount().getCommissionAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal pendingAmount = commissions.stream()
                .filter(c -> c.getStatus() == ReferralCommissionStatus.PENDING)
                .map(c -> c.getAmount().getCommissionAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal approvedAmount = commissions.stream()
                .filter(c -> c.getStatus() == ReferralCommissionStatus.APPROVED)
                .map(c -> c.getAmount().getCommissionAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal paidAmount = commissions.stream()
                .filter(c -> c.getStatus() == ReferralCommissionStatus.PAID)
                .map(c -> c.getAmount().getCommissionAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long pendingCount = commissions.stream()
                .filter(c -> c.getStatus() == ReferralCommissionStatus.PENDING).count();
        long approvedCount = commissions.stream()
                .filter(c -> c.getStatus() == ReferralCommissionStatus.APPROVED).count();
        long paidCount = commissions.stream()
                .filter(c -> c.getStatus() == ReferralCommissionStatus.PAID).count();

        // Calcular próximo pago (si hay aprobadas)
        // Lógica simple: siguiente 1 o 15 del mes
        BigDecimal nextPayoutAmount = approvedAmount;
        LocalDateTime nextPayoutDate = null;
        if (approvedAmount.compareTo(BigDecimal.ZERO) > 0) {
            nextPayoutDate = calculateNextPayoutDate();
        }

        // Historial reciente
        List<CommissionDto> recentCommissions = commissions.stream()
                .sorted((c1, c2) -> c2.getCreatedAt().compareTo(c1.getCreatedAt()))
                .limit(10)
                .map(this::mapToDto)
                .toList();

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
                recentCommissions);
    }

    private CommissionDto mapToDto(ReferralCommission c) {
        long daysRemaining = 0;
        if (c.getStatus() == ReferralCommissionStatus.PENDING) {
            LocalDateTime endsAt = c.getDisputePeriod().getDisputePeriodEndsAt();
            daysRemaining = ChronoUnit.DAYS.between(LocalDateTime.now(), endsAt);
            if (daysRemaining < 0)
                daysRemaining = 0;
        }

        return new CommissionDto(
                c.getId(),
                c.getBookingId(),
                c.getAmount().getCommissionRate(),
                c.getAmount().getCommissionAmount(),
                c.getAffiliatePlan(),
                c.getStatus().name(), // String status
                daysRemaining,
                c.isReadyForApproval(),
                c.getStatus() == ReferralCommissionStatus.APPROVED && !c.getDisputePeriod().isActive(), // Simplification
                                                                                                        // for
                                                                                                        // isPayable
                                                                                                        // logic
                c.getCreatedAt());
    }

    private LocalDateTime calculateNextPayoutDate() {
        LocalDateTime now = LocalDateTime.now();
        // Pagos el 1 y 15
        if (now.getDayOfMonth() < 15) {
            return now.withDayOfMonth(15).withHour(10).withMinute(0).withSecond(0);
        } else {
            return now.plusMonths(1).withDayOfMonth(1).withHour(10).withMinute(0).withSecond(0);
        }
    }

    private BigDecimal getCommissionRateForPlan(String plan) {
        if (plan == null)
            return BigDecimal.ZERO;
        return switch (plan.toUpperCase()) {
            case "FREE", "BASIC" -> new BigDecimal("0.02");
            case "PRO" -> new BigDecimal("0.05");
            case "ELITE", "PLUS" -> new BigDecimal("0.15");
            default -> BigDecimal.ZERO;
        };
    }
}