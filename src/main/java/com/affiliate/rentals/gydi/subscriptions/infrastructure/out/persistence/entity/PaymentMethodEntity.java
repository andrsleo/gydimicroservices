package com.affiliate.rentals.gydi.subscriptions.infrastructure.out.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * JPA entity representing a user's payment method.
 *
 * <p>
 * This entity maps to the {@code payment_methods} table in the
 * {@code subscriptions} schema.
 * It stores tokenized payment information from the payment gateway (e.g.,
 * Stripe).
 * It NEVER stores raw card numbers for PCI compliance.
 * </p>
 *
 * <p>
 * Security considerations:
 * </p>
 * <ul>
 * <li>Gateway tokens are used for processing payments securely</li>
 * <li>Only last 4 digits of card are stored for user reference</li>
 * <li>Full card details remain with the payment gateway</li>
 * </ul>
 *
 * <p>
 * This is an infrastructure-layer class that implements the persistence details
 * for the PaymentMethod domain model, following hexagonal architecture
 * principles.
 * </p>
 *
 * @author GYDI Development Team
 */
@Entity
@Table(name = "payment_methods", schema = "payment")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PaymentMethodEntity {

    /**
     * The unique identifier for this payment method.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The ID of the user who owns this payment method.
     * References users.users table.
     */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /**
     * The type of payment method.
     * Uses STRING mapping for H2 compatibility (tests) and PostgreSQL (production).
     * JdbcTypeCode VARCHAR allows PostgreSQL to auto-cast to ENUM during UPDATE.
     */
    @Enumerated(EnumType.STRING)
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.VARCHAR)
    @Column(name = "method_type", nullable = false)
    private PaymentMethodTypeEntity methodType;

    /**
     * The payment gateway provider (e.g., "STRIPE", "PAYPAL").
     */
    @Column(name = "gateway_provider", nullable = false, length = 50)
    private String gatewayProvider;

    /**
     * The tokenized payment method ID from the payment gateway.
     * This is the only payment data we store - never store raw card numbers!
     */
    @Column(name = "gateway_token", nullable = false, unique = true, length = 255)
    private String gatewayToken;

    /**
     * The last 4 digits of the card (for user reference only).
     */
    @Column(name = "card_last_four", columnDefinition = "varchar(4)")
    private String cardLastFour;

    /**
     * The card brand (e.g., "Visa", "Mastercard", "Amex").
     */
    @Column(name = "card_brand", length = 50)
    private String cardBrand;

    /**
     * The card expiration month (1-12).
     */
    @Column(name = "card_expiry_month")
    private Integer cardExpMonth;

    /**
     * The card expiration year (e.g., 2025).
     */
    @Column(name = "card_expiry_year")
    private Integer cardExpYear;

    /**
     * The billing email address.
     */
    @Column(name = "billing_email", length = 255)
    private String billingEmail;

    /**
     * Flag indicating if this is the default payment method for the user.
     */
    @Column(name = "is_default", nullable = false)
    private Boolean isDefault = false;

    /**
     * The current status of the payment method (ACTIVE, INACTIVE, EXPIRED, FAILED,
     * REPLACED).
     * This field implements soft delete functionality and lifecycle management.
     * JdbcTypeCode VARCHAR allows PostgreSQL to auto-cast to ENUM during UPDATE.
     */
    @Enumerated(EnumType.STRING)
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.VARCHAR)
    @Column(name = "status", nullable = false, length = 20)
    private PaymentMethodStatusEntity status = PaymentMethodStatusEntity.ACTIVE;

    /**
     * Timestamp when this payment method was soft deleted (status set to INACTIVE).
     * Null if payment method has never been deleted.
     */
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    /**
     * Timestamp when this payment method was created.
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Timestamp when this payment method was last updated.
     */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * Sets the creation timestamp before persisting.
     */
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    /**
     * Updates the modification timestamp before updating.
     */
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * Enum for payment method type (mirrors database enum).
     */
    public enum PaymentMethodTypeEntity {
        CREDIT_CARD, DEBIT_CARD, PAYPAL, STRIPE, OTHER
    }

    /**
     * Enum for payment method status (mirrors database enum).
     * This enum supports soft delete functionality and payment method lifecycle
     * management.
     */
    public enum PaymentMethodStatusEntity {
        ACTIVE, // Available for payments
        INACTIVE, // Soft deleted
        EXPIRED, // Card expired
        FAILED, // Gateway validation failed
        REPLACED // Replaced by another payment method
    }
}
