package com.affiliate.rentals.gydi.subscriptions.infrastructure.out.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

/**
 * JPA entity representing a user's subscription.
 *
 * <p>This entity maps to the {@code user_subscriptions} table in the {@code subscriptions} schema.
 * It stores the relationship between a user and their active subscription plan,
 * including status, payment method, and lifecycle dates.</p>
 *
 * <p>This is an infrastructure-layer class that implements the persistence details
 * for the UserSubscription domain model, following hexagonal architecture principles.</p>
 *
 * @author GYDI Development Team
 */
@Entity
@Table(name = "user_subscriptions", schema = "subscriptions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserSubscriptionEntity {

    /**
     * The unique identifier for this subscription.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The ID of the user who owns this subscription.
     * References users.users table.
     */
    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    /**
     * The ID of the subscription plan.
     */
    @Column(name = "plan_id", nullable = false)
    private Long planId;

    /**
     * The current status of the subscription.
     */
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)  // Fix PostgreSQL ENUM casting error
    @Column(name = "status", nullable = false, length = 50)
    private SubscriptionStatusEntity status = SubscriptionStatusEntity.ACTIVE;

    /**
     * The date when the subscription started.
     */
    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    /**
     * The date when the subscription expires (null for FREE plan).
     */
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    /**
     * The date when the subscription was canceled.
     */
    @Column(name = "canceled_at")
    private LocalDateTime canceledAt;

    /**
     * The reason for cancellation.
     */
    @Column(name = "cancellation_reason", length = 500)
    private String cancelationReason;

    /**
     * The ID of the payment method used for this subscription.
     */
    @Column(name = "payment_method_id")
    private Long paymentMethodId;

    /**
     * Flag indicating if the subscription should auto-renew.
     */
    @Column(name = "auto_renew", nullable = false)
    private Boolean autoRenew = true;

    /**
     * The date of the last renewal.
     */
    @Column(name = "last_renewal_at")
    private LocalDateTime lastRenewalAt;

    /**
     * The date of the next billing cycle.
     */
    @Column(name = "next_billing_date")
    private LocalDateTime nextBillingDate;

    /**
     * The Stripe subscription ID for payment gateway integration.
     */
    @Column(name = "stripe_subscription_id", unique = true, length = 255)
    private String stripeSubscriptionId;

    /**
     * Timestamp when this subscription was created.
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Timestamp when this subscription was last updated.
     */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * Sets the creation timestamp before persisting.
     */
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (startedAt == null) {
            startedAt = LocalDateTime.now();
        }
    }

    /**
     * Updates the modification timestamp before updating.
     */
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * Enum for subscription status (mirrors database enum).
     */
    public enum SubscriptionStatusEntity {
        ACTIVE, EXPIRED, CANCELED, SUSPENDED
    }
}
