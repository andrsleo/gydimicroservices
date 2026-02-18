package com.affiliate.rentals.gydi.referrals.infrastructure.out.persistence;

import com.affiliate.rentals.gydi.referrals.domain.model.ReferralLinkStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository para ReferralLink
 */
@Repository
public interface ReferralLinkJpaRepository extends JpaRepository<ReferralLinkJpaEntity, Long> {

    /**
     * Busca un enlace por token encriptado
     */
    Optional<ReferralLinkJpaEntity> findByEncryptedToken(String encryptedToken);

    /**
     * Busca un enlace por código corto
     */
    Optional<ReferralLinkJpaEntity> findByShortCode(String shortCode);

    /**
     * Busca todos los enlaces de un afiliado
     */
    List<ReferralLinkJpaEntity> findByAffiliateId(Long affiliateId);

    /**
     * Busca enlaces de un afiliado por estado
     */
    List<ReferralLinkJpaEntity> findByAffiliateIdAndStatus(Long affiliateId, ReferralLinkStatus status);

    /**
     * Busca enlaces de una propiedad
     */
    List<ReferralLinkJpaEntity> findByPropertyId(Long propertyId);

    /**
     * Busca enlaces activos que expiran en un rango de fechas
     */
    @Query("SELECT r FROM ReferralLinkJpaEntity r " +
            "WHERE r.status = 'ACTIVE' " +
            "AND r.deletedAt IS NULL " +
            "AND r.expiresAt BETWEEN :fromDate AND :toDate")
    List<ReferralLinkJpaEntity> findActiveLinksExpiringSoon(
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate);

    /**
     * Busca enlaces que ya expiraron pero aún no están marcados como EXPIRED
     */
    @Query("SELECT r FROM ReferralLinkJpaEntity r " +
            "WHERE r.status = 'ACTIVE' " +
            "AND r.deletedAt IS NULL " +
            "AND r.expiresAt < CURRENT_TIMESTAMP")
    List<ReferralLinkJpaEntity> findExpiredLinks();

    /**
     * Busca enlaces activos cuya fecha de expiración ya pasó
     * (status = ACTIVE AND expires_at < NOW())
     * Usado por el scheduler para marcarlos como EXPIRED automáticamente
     */
    @Query("SELECT r FROM ReferralLinkJpaEntity r " +
            "WHERE r.status = 'ACTIVE' " +
            "AND r.deletedAt IS NULL " +
            "AND r.expiresAt < CURRENT_TIMESTAMP")
    List<ReferralLinkJpaEntity> findActiveExpired();

    /**
     * Verifica si existe un enlace activo para una combinación afiliado-propiedad
     */
    @Query("SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END " +
            "FROM ReferralLinkJpaEntity r " +
            "WHERE r.affiliateId = :affiliateId " +
            "AND r.propertyId = :propertyId " +
            "AND r.status = 'ACTIVE' " +
            "AND r.deletedAt IS NULL")
    boolean existsActiveLink(
            @Param("affiliateId") Long affiliateId,
            @Param("propertyId") Long propertyId);

    /**
     * Busca un enlace activo para una combinación afiliado-propiedad
     */
    @Query("SELECT r FROM ReferralLinkJpaEntity r " +
            "WHERE r.affiliateId = :affiliateId " +
            "AND r.propertyId = :propertyId " +
            "AND r.status = 'ACTIVE' " +
            "AND r.deletedAt IS NULL")
    Optional<ReferralLinkJpaEntity> findActiveLink(
            @Param("affiliateId") Long affiliateId,
            @Param("propertyId") Long propertyId);

    /**
     * Cuenta el total de enlaces de un afiliado
     */
    long countByAffiliateId(Long affiliateId);
}