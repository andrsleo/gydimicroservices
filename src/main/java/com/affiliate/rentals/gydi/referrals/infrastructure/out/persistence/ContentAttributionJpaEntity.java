package com.affiliate.rentals.gydi.referrals.infrastructure.out.persistence;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "content_attributions", schema = "referrals")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ContentAttributionJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "booking_id")
    private Long bookingId;

    @Column(name = "content_post_id", nullable = false)
    private Long contentPostId;

    @Column(name = "creator_id", nullable = false)
    private Long creatorId;

    @Column(name = "referral_link_id")
    private Long referralLinkId;

    @Column(name = "attribution_type", nullable = false, length = 20)
    private String attributionType;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
