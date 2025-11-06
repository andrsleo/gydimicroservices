package com.affiliate.rentals.gydi.users.infrastructure.out.persistence.repository;

import com.affiliate.rentals.gydi.users.infrastructure.out.persistence.entity.PasswordResetAuditEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Spring Data JPA repository for PasswordResetAuditEntity.
 *
 * <p>This repository provides database access operations for password reset audit logs.
 * It extends JpaRepository to inherit standard CRUD operations and defines custom
 * queries for audit analysis.</p>
 *
 * <p>This is an infrastructure-layer interface following hexagonal architecture principles.
 * The actual implementation is provided automatically by Spring Data JPA.</p>
 *
 * @author GYDI Development Team
 * @see PasswordResetAuditEntity
 */
@Repository
public interface PasswordResetAuditJpaRepository extends JpaRepository<PasswordResetAuditEntity, Long> {

    /**
     * Finds all audit entries for a specific email address, ordered by creation date descending.
     *
     * @param userEmail the email address
     * @return list of audit entries for the email
     */
    List<PasswordResetAuditEntity> findByUserEmailOrderByCreatedAtDesc(String userEmail);

    /**
     * Finds audit entries for a specific email within a time range.
     *
     * @param userEmail the email address
     * @param since the start of the time range
     * @return list of audit entries
     */
    List<PasswordResetAuditEntity> findByUserEmailAndCreatedAtAfterOrderByCreatedAtDesc(
            String userEmail,
            LocalDateTime since
    );

    /**
     * Counts password reset requests for an email within a time window.
     * Used for rate limiting.
     *
     * @param userEmail the email address
     * @param action the action type (typically REQUEST)
     * @param since the start of the time window
     * @return the count of requests
     */
    @Query("""
            SELECT COUNT(a)
            FROM PasswordResetAuditEntity a
            WHERE a.userEmail = :userEmail
              AND a.action = :action
              AND a.createdAt >= :since
            """)
    int countByUserEmailAndActionSince(
            @Param("userEmail") String userEmail,
            @Param("action") PasswordResetAuditEntity.PasswordResetAction action,
            @Param("since") LocalDateTime since
    );

    /**
     * Finds recent failed attempts for an email (for security monitoring).
     *
     * @param userEmail the email address
     * @param since the start of the time window
     * @return list of failed audit entries
     */
    @Query("""
            SELECT a
            FROM PasswordResetAuditEntity a
            WHERE a.userEmail = :userEmail
              AND a.success = false
              AND a.createdAt >= :since
            ORDER BY a.createdAt DESC
            """)
    List<PasswordResetAuditEntity> findFailedAttemptsSince(
            @Param("userEmail") String userEmail,
            @Param("since") LocalDateTime since
    );
}
