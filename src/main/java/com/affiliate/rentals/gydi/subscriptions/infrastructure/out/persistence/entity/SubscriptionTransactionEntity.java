package com.affiliate.rentals.gydi.subscriptions.infrastructure.out.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * JPA entity representing a subscription transaction.
 *
 * <p>This entity maps to the {@code subscription_transactions} table in the {@code subscriptions} schema.
 * It records all financial transactions related to subscriptions, including upgrades, renewals,
 * cancellations, and refunds.</p>
 *
 * <p>Each transaction is linked to a payment gateway transaction (e.g., Stripe) and
 * maintains a complete audit trail of subscription changes.</p>
 *
 * <p>This is an infrastructure-layer class that implements the persistence details
 * for the SubscriptionTransaction domain model, following hexagonal architecture principles.</p>
 *
 * @author GYDI Development Team
 */
@Entity
@Table(name = "subscription_transactions", schema = "subscriptions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionTransactionEntity {

    /**
     * The unique identifier for this transaction.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The ID of the user subscription this transaction belongs to.
     */
    @Column(name = "user_subscription_id", nullable = false)
    private Long userSubscriptionId;

    /**
     * The ID of the user (denormalized for fast queries).
     */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /**
     * The ID of the payment method used for this transaction.
     */
    @Column(name = "payment_method_id")
    private Long paymentMethodId;

    /**
     * The type of transaction (e.g., UPGRADE, RENEWAL, CANCELLATION).
     */
    @Column(name = "transaction_type", nullable = false)
    private TransactionTypeEntity transactionType;

    /**
     * The current status of the transaction (e.g., PENDING, COMPLETED, FAILED).
     */
    @Column(name = "transaction_status", nullable = false)
    private TransactionStatusEntity transactionStatus = TransactionStatusEntity.PENDING;

    /**
     * The ID of the plan being changed from (for plan changes).
     */
    @Column(name = "from_plan_id")
    private Long fromPlanId;

    /**
     * The ID of the plan being changed to (for plan changes).
     */
    @Column(name = "to_plan_id")
    private Long toPlanId;

    /**
     * The transaction amount.
     */
    @Column(name = "amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    /**
     * The currency code (e.g., "USD").
     */
    @Column(name = "currency", nullable = false, length = 3)
    private String currency = "USD";

    /**
     * The payment gateway provider (e.g., 'stripe', 'paypal').
     */
    @Column(name = "gateway_provider", length = 50)
    private String gatewayProvider;

    /**
     * The generic transaction ID from the payment gateway.
     */
    @Column(name = "gateway_transaction_id", length = 255)
    private String gatewayTransactionId;

    /**
     * The Stripe Payment Intent ID.
     */
    @Column(name = "stripe_payment_intent_id", unique = true, length = 255)
    private String stripePaymentIntentId;

    /**
     * The Stripe Charge ID (after payment is confirmed).
     */
    @Column(name = "stripe_charge_id", length = 255)
    private String stripeChargeId;

    /**
     * The Stripe Invoice ID (for subscription renewals).
     */
    @Column(name = "stripe_invoice_id", length = 255)
    private String stripeInvoiceId;

    /**
     * The raw response from the payment gateway (for debugging/audit).
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "gateway_response")
    private String gatewayResponse;

    /**
     * The reason for failure (if transaction failed).
     */
    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    /**
     * The number of retry attempts for this transaction.
     */
    @Column(name = "retry_count", nullable = false)
    private Integer retryCount = 0;

    /**
     * The start of the subscription period covered by this transaction.
     */
    @Column(name = "period_start", nullable = false)
    private LocalDateTime periodStart;

    /**
     * The end of the subscription period covered by this transaction.
     */
    @Column(name = "period_end")
    private LocalDateTime periodEnd;

    /**
     * Additional metadata (promo codes, discounts, etc.) stored as JSONB.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata")
    private String metadata;

    /**
     * The date when the transaction was completed.
     */
    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    /**
     * The date when the transaction was processed/completed (synonym for completed_at).
     */
    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    /**
     * Timestamp when this transaction was created.
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Timestamp when this transaction was last updated.
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

    /**
     * Enum for transaction type (mirrors database enum).
     */
    public enum TransactionTypeEntity {
        INITIAL_SUBSCRIPTION, UPGRADE, RENEWAL, DOWNGRADE, CANCELLATION, REACTIVATION
    }

    /**
     * Enum for transaction status (mirrors database enum).
     */
    public enum TransactionStatusEntity {
        PENDING, PROCESSING, COMPLETED, FAILED, REFUNDED, CANCELED
    }
}
