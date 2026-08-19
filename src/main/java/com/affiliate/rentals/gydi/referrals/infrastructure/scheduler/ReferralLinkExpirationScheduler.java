package com.affiliate.rentals.gydi.referrals.infrastructure.scheduler;

import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.affiliate.rentals.gydi.referrals.domain.model.ReferralLink;
import com.affiliate.rentals.gydi.referrals.domain.port.ReferralLinkRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Scheduler para marcar automáticamente enlaces de referido como expirados
 *
 * Ejecuta cada hora para buscar enlaces con status ACTIVE cuya fecha
 * de expiración ya haya pasado y los marca como EXPIRED.
 *
 * FASE 1: Expiración automática basada en plan
 * - Busca links con status='ACTIVE' AND expires_at < NOW()
 * - Los marca como EXPIRED
 * - Se ejecuta cada hora (cron: "0 0 * * * *")
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReferralLinkExpirationScheduler {

    private final ReferralLinkRepository referralLinkRepository;

    /**
     * Tarea programada que se ejecuta cada hora
     * Cron: "0 0 * * * *" = segundo 0, minuto 0, cada hora
     */
    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void expireActiveLinks() {
        log.info("Running scheduled task: Expire active referral links");

        try {
            // Buscar enlaces activos cuya expiración ya pasó
            List<ReferralLink> expiredLinks = referralLinkRepository.findActiveExpired();

            if (expiredLinks.isEmpty()) {
                log.debug("No active links found to expire");
                return;
            }

            log.info("Found {} active link(s) to expire", expiredLinks.size());

            int expiredCount = 0;
            for (ReferralLink link : expiredLinks) {
                try {
                    // Marcar como expirado (método de dominio)
                    link.markAsExpired();

                    // Persistir cambio
                    referralLinkRepository.update(link);

                    expiredCount++;

                    log.debug("Marked link as expired: ID={}, AffiliateId={}, PropertyId={}, ExpiresAt={}",
                            link.getId(), link.getAffiliateId(), link.getPropertyId(), link.getExpiresAt());

                } catch (Exception e) {
                    log.error("Error expiring link ID {}: {}", link.getId(), e.getMessage(), e);
                    // Continuar con el siguiente link aunque uno falle
                }
            }

            log.info("Successfully expired {} link(s)", expiredCount);

        } catch (Exception e) {
            log.error("Error in scheduled task expireActiveLinks: {}", e.getMessage(), e);
        }
    }
}
