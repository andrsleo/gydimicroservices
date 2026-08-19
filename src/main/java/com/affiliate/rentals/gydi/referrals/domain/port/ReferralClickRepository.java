package com.affiliate.rentals.gydi.referrals.domain.port;

import com.affiliate.rentals.gydi.referrals.domain.model.DeviceType;
import com.affiliate.rentals.gydi.referrals.domain.model.ReferralClick;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Puerto de salida para persistencia de clicks en enlaces de referido
 */
public interface ReferralClickRepository {

    /**
     * Guarda un nuevo click
     */
    ReferralClick save(ReferralClick click);

    /**
     * Busca un click por ID
     */
    Optional<ReferralClick> findById(Long id);

    /**
     * Busca todos los clicks de un enlace de referido
     */
    List<ReferralClick> findByReferralLinkId(Long referralLinkId);

    /**
     * Busca clicks de un enlace en un rango de fechas
     */
    List<ReferralClick> findByReferralLinkIdAndDateRange(Long referralLinkId,
            LocalDateTime from,
            LocalDateTime to);

    /**
     * Cuenta clicks de un enlace
     */
    long countByReferralLinkId(Long referralLinkId);

    /**
     * Cuenta clicks de un enlace en un rango de fechas
     */
    long countByReferralLinkIdAndDateRange(Long referralLinkId,
            LocalDateTime from,
            LocalDateTime to);

    /**
     * Busca clicks sospechosos de ser bots (botScore >= threshold)
     */
    List<ReferralClick> findProbableBots(int botScoreThreshold, LocalDateTime from, LocalDateTime to);

    /**
     * Agrupa clicks por tipo de dispositivo para un enlace
     */
    java.util.Map<DeviceType, Long> countByDeviceType(Long referralLinkId);

    /**
     * Agrupa clicks por país para un enlace
     */
    java.util.Map<String, Long> countByCountry(Long referralLinkId);

    /**
     * Busca clicks duplicados (mismo hash IP + hash UA en ventana de tiempo corta)
     */
    List<ReferralClick> findDuplicateClicks(Long referralLinkId,
            byte[] ipHash,
            byte[] userAgentHash,
            LocalDateTime since);

    /**
     * Elimina clicks antiguos (GDPR - retención de 90 días)
     */
    int deleteClicksOlderThan(LocalDateTime beforeDate);
}