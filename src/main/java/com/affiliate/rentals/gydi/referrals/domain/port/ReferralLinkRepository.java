package com.affiliate.rentals.gydi.referrals.domain.port;

import com.affiliate.rentals.gydi.referrals.domain.model.ReferralLink;
import com.affiliate.rentals.gydi.referrals.domain.model.ReferralLinkStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Puerto de salida para persistencia de enlaces de referido
 *
 * Define el contrato para operaciones de persistencia sin
 * acoplarse a una implementación específica (JPA, MongoDB, etc.)
 */
public interface ReferralLinkRepository {

    /**
     * Guarda un nuevo enlace de referido
     */
    ReferralLink save(ReferralLink referralLink);

    /**
     * Actualiza un enlace de referido existente
     */
    ReferralLink update(ReferralLink referralLink);

    /**
     * Busca un enlace por ID
     */
    Optional<ReferralLink> findById(Long id);

    /**
     * Busca un enlace por token encriptado
     */
    Optional<ReferralLink> findByEncryptedToken(String encryptedToken);

    /**
     * Busca un enlace por código corto
     */
    Optional<ReferralLink> findByShortCode(String shortCode);

    /**
     * Busca todos los enlaces de un afiliado
     */
    List<ReferralLink> findByAffiliateId(Long affiliateId);

    /**
     * Busca enlaces de un afiliado por estado
     */
    List<ReferralLink> findByAffiliateIdAndStatus(Long affiliateId, ReferralLinkStatus status);

    /**
     * Busca enlaces de una propiedad
     */
    List<ReferralLink> findByPropertyId(Long propertyId);

    /**
     * Busca enlaces activos que expiran pronto (próximos 7 días)
     */
    List<ReferralLink> findActiveLinksExpiringSoon(LocalDateTime fromDate, LocalDateTime toDate);

    /**
     * Busca enlaces que ya expiraron pero aún no están marcados como EXPIRED
     */
    List<ReferralLink> findExpiredLinks();

    /**
     * Verifica si existe un enlace activo para una combinación afiliado-propiedad
     */
    boolean existsActiveLink(Long affiliateId, Long propertyId);

    /**
     * Busca un enlace activo para una combinación afiliado-propiedad
     */
    Optional<ReferralLink> findActiveLink(Long affiliateId, Long propertyId);

    /**
     * Cuenta el total de enlaces de un afiliado
     */
    long countByAffiliateId(Long affiliateId);

    /**
     * Elimina (físicamente) un enlace por ID
     */
    void deleteById(Long id);
}