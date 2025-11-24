package com.affiliate.rentals.gydi.referrals.infrastructure.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Spring Data JPA repository para ReferralClick
 */
@Repository
public interface ReferralClickJpaRepository extends JpaRepository<ReferralClickJpaEntity, Long> {

    /**
     * Busca todos los clicks de un enlace de referido
     */
    List<ReferralClickJpaEntity> findByReferralLinkId(Long referralLinkId);

    /**
     * Busca clicks de un enlace en un rango de fechas
     */
    @Query("SELECT c FROM ReferralClickJpaEntity c " +
           "WHERE c.referralLinkId = :referralLinkId " +
           "AND c.clickedAt BETWEEN :from AND :to")
    List<ReferralClickJpaEntity> findByReferralLinkIdAndDateRange(
        @Param("referralLinkId") Long referralLinkId,
        @Param("from") LocalDateTime from,
        @Param("to") LocalDateTime to
    );

    /**
     * Cuenta clicks de un enlace
     */
    long countByReferralLinkId(Long referralLinkId);

    /**
     * Cuenta clicks de un enlace en un rango de fechas
     */
    @Query("SELECT COUNT(c) FROM ReferralClickJpaEntity c " +
           "WHERE c.referralLinkId = :referralLinkId " +
           "AND c.clickedAt BETWEEN :from AND :to")
    long countByReferralLinkIdAndDateRange(
        @Param("referralLinkId") Long referralLinkId,
        @Param("from") LocalDateTime from,
        @Param("to") LocalDateTime to
    );

    /**
     * Busca clicks sospechosos de ser bots
     */
    @Query("SELECT c FROM ReferralClickJpaEntity c " +
           "WHERE c.botScore >= :botScoreThreshold " +
           "AND c.clickedAt BETWEEN :from AND :to")
    List<ReferralClickJpaEntity> findProbableBots(
        @Param("botScoreThreshold") int botScoreThreshold,
        @Param("from") LocalDateTime from,
        @Param("to") LocalDateTime to
    );

    /**
     * Busca clicks duplicados
     */
    @Query("SELECT c FROM ReferralClickJpaEntity c " +
           "WHERE c.referralLinkId = :referralLinkId " +
           "AND c.ipAddressHash = :ipHash " +
           "AND c.userAgentHash = :userAgentHash " +
           "AND c.clickedAt >= :since")
    List<ReferralClickJpaEntity> findDuplicateClicks(
        @Param("referralLinkId") Long referralLinkId,
        @Param("ipHash") byte[] ipHash,
        @Param("userAgentHash") byte[] userAgentHash,
        @Param("since") LocalDateTime since
    );

    /**
     * Elimina clicks antiguos (GDPR - retención de 90 días)
     */
    @Modifying
    @Query("DELETE FROM ReferralClickJpaEntity c WHERE c.clickedAt < :beforeDate")
    int deleteClicksOlderThan(@Param("beforeDate") LocalDateTime beforeDate);
}