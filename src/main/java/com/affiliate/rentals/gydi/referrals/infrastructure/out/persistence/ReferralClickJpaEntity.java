package com.affiliate.rentals.gydi.referrals.infrastructure.out.persistence;

import com.affiliate.rentals.gydi.referrals.domain.model.DeviceType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.PostgreSQLEnumJdbcType;

import java.time.LocalDateTime;

/**
 * Entidad JPA para referral_clicks (tabla particionada)
 */
@Entity
@Table(name = "referral_clicks", schema = "referrals")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReferralClickJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "referral_link_id", nullable = false)
    private Long referralLinkId;

    @Column(name = "ip_address_hash", nullable = false)
    private byte[] ipAddressHash;

    @Column(name = "user_agent_hash", nullable = false)
    private byte[] userAgentHash;

    @Column(name = "fingerprint", length = 64)
    private String fingerprint;

    @Column(name = "country_code", length = 2)
    private String countryCode;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(name = "device_type", nullable = false, columnDefinition = "referrals.device_type")
    private DeviceType deviceType = DeviceType.UNKNOWN;

    @Column(name = "bot_score")
    private Integer botScore;

    @CreationTimestamp
    @Column(name = "clicked_at", nullable = false)
    private LocalDateTime clickedAt;
}