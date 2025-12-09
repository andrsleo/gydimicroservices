package com.affiliate.rentals.gydi.referrals.application.usecase;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.affiliate.rentals.gydi.referrals.application.dto.ReferralStatsDto;
import com.affiliate.rentals.gydi.referrals.domain.model.CommissionStatus;
import com.affiliate.rentals.gydi.referrals.domain.model.ReferralLink;
import com.affiliate.rentals.gydi.referrals.domain.model.ReferralLinkStatus;
import com.affiliate.rentals.gydi.referrals.domain.port.CommissionRepository;
import com.affiliate.rentals.gydi.referrals.domain.port.ReferralClickRepository;
import com.affiliate.rentals.gydi.referrals.domain.port.ReferralLinkRepository;

import lombok.extern.slf4j.Slf4j;


/**
 * Caso de uso para obtener estadísticas del sistema de referidos de un afiliado
 */
@Slf4j
@Service
@Transactional(readOnly = true)
public class GetReferralStatsUseCase {

    private final ReferralLinkRepository referralLinkRepository;
    private final ReferralClickRepository referralClickRepository;
    private final CommissionRepository commissionRepository;

    public GetReferralStatsUseCase(ReferralLinkRepository referralLinkRepository,
                                    ReferralClickRepository referralClickRepository,
                                    CommissionRepository commissionRepository) {
        this.referralLinkRepository = referralLinkRepository;
        this.referralClickRepository = referralClickRepository;
        this.commissionRepository = commissionRepository;
    }

    /**
     * Obtiene estadísticas completas para un afiliado
     */
    public ReferralStatsDto execute(Long affiliateId) {
        log.debug("Getting referral stats for affiliate: {}", affiliateId);

        // Obtener todos los enlaces del afiliado
        List<ReferralLink> allLinks = referralLinkRepository.findByAffiliateId(affiliateId);

        // Estadísticas de enlaces
        int totalLinks = allLinks.size();
        int activeLinks = (int) allLinks.stream()
            .filter(link -> link.getStatus() == ReferralLinkStatus.ACTIVE)
            .count();
        int expiredLinks = (int) allLinks.stream()
            .filter(link -> link.getStatus() == ReferralLinkStatus.EXPIRED)
            .count();

        // Estadísticas de clicks
        long totalClicks = allLinks.stream()
            .mapToLong(ReferralLink::getClicksCount)
            .sum();

        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        long clicksLast30Days = allLinks.stream()
            .mapToLong(link ->
                referralClickRepository.countByReferralLinkIdAndDateRange(
                    link.getId(),
                    thirtyDaysAgo,
                    LocalDateTime.now()
                )
            )
            .sum();

        // TODO: Implementar agregación por país y dispositivo
        Map<String, Long> clicksByCountry = new HashMap<>();
        Map<String, Long> clicksByDevice = new HashMap<>();

        // Estadísticas de conversiones (bookings completados)
        // TODO: Implementar conteo de bookings por affiliate_id desde tabla booking
        int totalConversions = 0; // Se calculará desde bookings en el futuro
        int conversionsLast30Days = 0;

        double overallConversionRate = totalClicks > 0
            ? (double) totalConversions / totalClicks * 100
            : 0.0;

        // Estadísticas financieras
        BigDecimal totalEarnings = commissionRepository.calculateTotalEarningsByAffiliateId(affiliateId);
        BigDecimal pendingCommissions = commissionRepository.calculateEarningsByAffiliateIdAndStatus(
            affiliateId, CommissionStatus.PENDING);
        BigDecimal approvedCommissions = commissionRepository.calculateEarningsByAffiliateIdAndStatus(
            affiliateId, CommissionStatus.APPROVED);
        BigDecimal paidCommissions = commissionRepository.calculateEarningsByAffiliateIdAndStatus(
            affiliateId, CommissionStatus.PAID);

        // TODO: Implementar earningsByMonth si es necesario
        Map<String, BigDecimal> earningsByMonth = new HashMap<>();

        return new ReferralStatsDto(
            affiliateId,
            totalLinks,
            activeLinks,
            expiredLinks,
            totalClicks,
            totalClicks, // uniqueClicks (por ahora igual a totalClicks)
            clicksLast30Days,
            clicksByCountry,
            clicksByDevice,
            totalConversions,
            conversionsLast30Days,
            overallConversionRate,
            totalEarnings,
            pendingCommissions,
            approvedCommissions,
            paidCommissions,
            earningsByMonth
        );
    }
}