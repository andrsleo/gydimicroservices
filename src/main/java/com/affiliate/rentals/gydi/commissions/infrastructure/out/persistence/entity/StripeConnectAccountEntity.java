package com.affiliate.rentals.gydi.commissions.infrastructure.out.persistence.entity;

import com.affiliate.rentals.gydi.commissions.domain.model.ConnectAccountType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * JPA Entity for Stripe Connect Account.
 * <p>
 * Maps to table: commissions.stripe_connect_accounts (created in V81 migration)
 * </p>
 */
@Entity
@Table(name = "stripe_connect_accounts", schema = "commissions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StripeConnectAccountEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(name = "stripe_account_id", nullable = false, unique = true, length = 255)
    private String stripeAccountId;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false, length = 20)
    private ConnectAccountType accountType;

    @Column(name = "onboarding_completed", nullable = false)
    private Boolean onboardingCompleted;

    @Column(name = "details_submitted", nullable = false)
    private Boolean detailsSubmitted;

    @Column(name = "payouts_enabled", nullable = false)
    private Boolean payoutsEnabled;

    @Column(name = "charges_enabled", nullable = false)
    private Boolean chargesEnabled;

    @Column(name = "country", length = 2)
    private String country;

    @Column(name = "verification_status", length = 50)
    private String verificationStatus;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;

        // Set defaults
        if (this.onboardingCompleted == null) {
            this.onboardingCompleted = false;
        }
        if (this.detailsSubmitted == null) {
            this.detailsSubmitted = false;
        }
        if (this.payoutsEnabled == null) {
            this.payoutsEnabled = false;
        }
        if (this.chargesEnabled == null) {
            this.chargesEnabled = false;
        }
        if (this.verificationStatus == null) {
            this.verificationStatus = "unverified";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
