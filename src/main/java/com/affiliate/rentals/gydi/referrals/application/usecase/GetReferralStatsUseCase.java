package com.affiliate.rentals.gydi.referrals.application.usecase;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.affiliate.rentals.gydi.bookings.domain.model.Booking;
import com.affiliate.rentals.gydi.bookings.domain.model.BookingStatus;
import com.affiliate.rentals.gydi.bookings.domain.ports.BookingRepositoryPort;
import com.affiliate.rentals.gydi.commissions.domain.model.ReferralCommission;
import com.affiliate.rentals.gydi.commissions.domain.model.ReferralCommissionStatus;
import com.affiliate.rentals.gydi.commissions.domain.ports.ReferralCommissionRepositoryPort;
import com.affiliate.rentals.gydi.referrals.application.dto.ReferralStatsDto;
import com.affiliate.rentals.gydi.referrals.domain.model.ReferralLink;
import com.affiliate.rentals.gydi.referrals.domain.model.ReferralLinkStatus;
import com.affiliate.rentals.gydi.referrals.domain.port.ReferralClickRepository;
import com.affiliate.rentals.gydi.referrals.domain.port.ReferralLinkRepository;

import lombok.extern.slf4j.Slf4j;

/**
 * Caso de uso para obtener estadísticas del sistema de referidos de un
 * referido.
 * 
 * Actualizado para usar los nuevos repositorios:
 * - bookings.booking (via BookingRepositoryPort) para conversiones.
 * - commissions (via AffiliateCommissionRepositoryPort) para dinero.
 */
@Slf4j
@Service
@Transactional(readOnly = true)
public class GetReferralStatsUseCase {

        private final ReferralLinkRepository referralLinkRepository;
        private final ReferralClickRepository referralClickRepository;
        private final BookingRepositoryPort bookingRepository;
        private final ReferralCommissionRepositoryPort commissionRepository;

        public GetReferralStatsUseCase(ReferralLinkRepository referralLinkRepository,
                        ReferralClickRepository referralClickRepository,
                        BookingRepositoryPort bookingRepository,
                        ReferralCommissionRepositoryPort commissionRepository) {
                this.referralLinkRepository = referralLinkRepository;
                this.referralClickRepository = referralClickRepository;
                this.bookingRepository = bookingRepository;
                this.commissionRepository = commissionRepository;
        }

        /**
         * Obtiene estadísticas completas para un referido
         */
        public ReferralStatsDto execute(Long affiliateId) {
                log.debug("Getting referral stats for affiliate: {}", affiliateId);

                // 1. Obtener todos los enlaces del referido
                List<ReferralLink> allLinks = referralLinkRepository.findByAffiliateId(affiliateId);

                // Estadísticas de enlaces
                int totalLinks = allLinks.size();
                int activeLinks = (int) allLinks.stream()
                                .filter(link -> link.getStatus() == ReferralLinkStatus.ACTIVE)
                                .count();
                int expiredLinks = (int) allLinks.stream()
                                .filter(link -> link.getStatus() == ReferralLinkStatus.EXPIRED)
                                .count();

                // 2. Estadísticas de clicks (Legacy referral tables - kept)
                long totalClicks = allLinks.stream()
                                .mapToLong(ReferralLink::getClicksCount)
                                .sum();

                LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
                long clicksLast30Days = allLinks.stream()
                                .mapToLong(link -> referralClickRepository.countByReferralLinkIdAndDateRange(
                                                link.getId(),
                                                thirtyDaysAgo,
                                                LocalDateTime.now()))
                                .sum();

                // TODO: Implementar agregación por país y dispositivo
                Map<String, Long> clicksByCountry = new HashMap<>();
                Map<String, Long> clicksByDevice = new HashMap<>();

                // 3. Estadísticas de conversiones (NUEVO: Usando bookings.booking)
                int totalConversions = 0;
                int conversionsLast30Days = 0; // Simple approximation or implement query

                // Iterar links para contar bookings (Nota: esto podría optimizarse con un query
                // en BookingRepositoryPort)
                // Actualmente BookingRepositoryPort.findByReferralLinkId devuelve lista.
                for (ReferralLink link : allLinks) {
                        List<Booking> bookings = bookingRepository.findByReferralLinkId(link.getId());

                        // Contamos como conversión bookings CONFIRMADOS o FINALIZADOS o EN PROGRESO
                        List<Booking> convertedBookings = bookings.stream()
                                        .filter(b -> b.getStatus() == BookingStatus.FINISHED ||
                                                        b.getStatus() == BookingStatus.IN_PROGRESS ||
                                                        b.getStatus() == BookingStatus.RESERVED)
                                        .toList();

                        totalConversions += convertedBookings.size();

                        // Conversiones último mes (aprox usando created_at)
                        conversionsLast30Days += convertedBookings.stream()
                                        .filter(b -> b.getCreatedAt().isAfter(thirtyDaysAgo))
                                        .count();
                }

                double overallConversionRate = totalClicks > 0
                                ? (double) totalConversions / totalClicks * 100
                                : 0.0;

                // 4. Estadísticas financieras (NUEVO: Usando commissions module)
                List<ReferralCommission> commissions = commissionRepository.findByAffiliateId(affiliateId);

                BigDecimal totalEarnings = commissions.stream()
                                .map(c -> c.getAmount().getCommissionAmount())
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                BigDecimal pendingCommissions = commissions.stream()
                                .filter(c -> c.getStatus() == ReferralCommissionStatus.PENDING)
                                .map(c -> c.getAmount().getCommissionAmount())
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                BigDecimal approvedCommissions = commissions.stream()
                                .filter(c -> c.getStatus() == ReferralCommissionStatus.APPROVED)
                                .map(c -> c.getAmount().getCommissionAmount())
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                BigDecimal paidCommissions = commissions.stream()
                                .filter(c -> c.getStatus() == ReferralCommissionStatus.PAID)
                                .map(c -> c.getAmount().getCommissionAmount())
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                // Earnings by Month (agrupado por created_at)
                Map<String, BigDecimal> earningsByMonth = commissions.stream()
                                .collect(Collectors.groupingBy(
                                                c -> c.getCreatedAt().getMonth().toString() + " "
                                                                + c.getCreatedAt().getYear(),
                                                Collectors.reducing(BigDecimal.ZERO,
                                                                c -> c.getAmount().getCommissionAmount(),
                                                                BigDecimal::add)));

                return new ReferralStatsDto(
                                affiliateId,
                                totalLinks,
                                activeLinks,
                                expiredLinks,
                                totalClicks,
                                totalClicks, // uniqueClicks
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
                                earningsByMonth);
        }
}