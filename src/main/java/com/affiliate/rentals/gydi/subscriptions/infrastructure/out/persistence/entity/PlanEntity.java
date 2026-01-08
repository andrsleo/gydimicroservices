package com.affiliate.rentals.gydi.subscriptions.infrastructure.out.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * JPA entity representing a subscription plan.
 *
 * <p>This entity maps to the {@code subscription_plans} table in the {@code subscriptions} schema.
 * It stores plan information including pricing, commission rates, and feature limits.</p>
 *
 * <p>This is an infrastructure-layer class that implements the persistence details
 * for the Plan domain model, following hexagonal architecture principles.</p>
 *
 * @author GYDI Development Team
 */
@Entity
@Table(name = "subscription_plans", schema = "subscriptions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PlanEntity {

    /**
     * The unique identifier for this plan.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The unique code for this plan (e.g., "FREE", "PRO", "ELITE").
     * Must be unique and cannot be null.
     */
    @Column(name = "plan_code", nullable = false, unique = true, length = 20)
    private String planCode;

    /**
     * The display name of the plan.
     */
    @Column(name = "plan_name", nullable = false, length = 100)
    private String planName;

    /**
     * A description of the plan and its features.
     */
    @Column(name = "plan_description", columnDefinition = "TEXT")
    private String planDescription;

    /**
     * The monthly price of the plan.
     */
    @Column(name = "monthly_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal monthlyPrice;

    /**
     * The currency code (e.g., "USD").
     */
    @Column(name = "currency", nullable = false, length = 3)
    private String currency = "USD";

    /**
     * The commission rate for referrals (as decimal, e.g., 0.02 for 2%).
     */
    @Column(name = "commission_rate", nullable = false, precision = 5, scale = 4)
    private BigDecimal commissionRate;

    /**
     * Maximum number of referrals that can be generated per month.
     */
    @Column(name = "referral_limit_per_month", nullable = false)
    private Integer referralLimitPerMonth;

    /**
     * Maximum number of properties that can be published.
     */
    @Column(name = "property_publish_limit", nullable = false)
    private Integer propertyPublishLimit;

    /**
     * Flag indicating if the plan is currently active and available for subscription.
     */
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    /**
     * Flag indicating if the plan should be featured/highlighted in UI.
     */
    @Column(name = "is_featured", nullable = false)
    private Boolean isFeatured = false;

    /**
     * The display order for sorting plans in UI.
     */
    @Column(name = "display_order")
    private Integer displayOrder;

    /**
     * The Stripe Price ID (price_xxxxx).
     * Links this plan to its Stripe Price record.
     * Created when plan is synced with Stripe.
     * NULL for FREE plan or plans not yet synced.
     */
    @Column(name = "stripe_price_id", unique = true, length = 255)
    private String stripePriceId;

    /**
     * Timestamp when this plan was created.
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Timestamp when this plan was last updated.
     */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * Sets the creation timestamp before persisting.
     */
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    /**
     * Updates the modification timestamp before updating.
     */
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
