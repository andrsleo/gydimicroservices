package com.affiliate.rentals.gydi.referrals.infrastructure.out.persistence;

import com.affiliate.rentals.gydi.referrals.domain.model.ReferralLinkStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcType;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.dialect.PostgreSQLEnumJdbcType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entidad JPA para referral_links
 */
@Entity
@Table(name = "referral_links", schema = "referrals")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReferralLinkJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "affiliate_id", nullable = false)
    private Long affiliateId;

    @Column(name = "property_id", nullable = false)
    private Long propertyId;

    @Column(name = "encrypted_token", nullable = false, unique = true)
    private String encryptedToken;

    @Column(name = "short_code", length = 10, unique = true)
    private String shortCode;

    @Column(name = "clicks_count", nullable = false)
    private Integer clicksCount = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, columnDefinition = "referrals.referral_link_status")
    @JdbcType(PostgreSQLEnumJdbcType.class)
    private ReferralLinkStatus status = ReferralLinkStatus.ACTIVE;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}