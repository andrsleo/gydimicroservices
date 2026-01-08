package com.affiliate.rentals.gydi.subscriptions.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Domain model representing a subscription transaction.
 *
 * <p>This immutable entity records all financial transactions related to subscriptions,
 * including upgrades, renewals, cancellations, and refunds.</p>
 *
 * <p>Each transaction is linked to a payment gateway transaction (e.g., Stripe) and
 * maintains a complete audit trail of subscription changes.</p>
 *
 * @author GYDI Development Team
 */
public final class SubscriptionTransaction {
    private final Long id;
    private final Long userSubscriptionId;
    private final Long userId;
    private final Long paymentMethodId;
    private final TransactionType transactionType;
    private final TransactionStatus transactionStatus;
    private final Long fromPlanId;
    private final Long toPlanId;
    private final BigDecimal amount;
    private final String currency;
    private final String stripePaymentIntentId;
    private final String stripeChargeId;
    private final String stripeInvoiceId;
    private final String gatewayResponse;
    private final String failureReason;
    private final LocalDateTime periodStart;
    private final LocalDateTime periodEnd;
    private final LocalDateTime processedAt;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    private SubscriptionTransaction(Builder builder) {
        this.id = builder.id;
        this.userSubscriptionId = Objects.requireNonNull(builder.userSubscriptionId, "User subscription ID cannot be null");
        this.userId = Objects.requireNonNull(builder.userId, "User ID cannot be null");
        this.paymentMethodId = builder.paymentMethodId;
        this.transactionType = Objects.requireNonNull(builder.transactionType, "Transaction type cannot be null");
        this.transactionStatus = Objects.requireNonNull(builder.transactionStatus, "Transaction status cannot be null");
        this.fromPlanId = builder.fromPlanId;
        this.toPlanId = builder.toPlanId;
        this.amount = Objects.requireNonNull(builder.amount, "Amount cannot be null");
        this.currency = Objects.requireNonNullElse(builder.currency, "USD");
        this.stripePaymentIntentId = builder.stripePaymentIntentId;
        this.stripeChargeId = builder.stripeChargeId;
        this.stripeInvoiceId = builder.stripeInvoiceId;
        this.gatewayResponse = builder.gatewayResponse;
        this.failureReason = builder.failureReason;
        this.periodStart = Objects.requireNonNull(builder.periodStart, "Period start cannot be null");
        this.periodEnd = builder.periodEnd;
        this.processedAt = builder.processedAt;
        this.createdAt = Objects.requireNonNullElseGet(builder.createdAt, LocalDateTime::now);
        this.updatedAt = builder.updatedAt;

        validateBusinessRules();
    }

    private void validateBusinessRules() {
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Transaction amount cannot be negative");
        }

        if (transactionType.changesPlanTier() && (fromPlanId == null || toPlanId == null)) {
            throw new IllegalArgumentException("Plan change transactions must have both from and to plan IDs");
        }

        if (transactionStatus == TransactionStatus.FAILED && failureReason == null) {
            throw new IllegalStateException("Failed transactions must have a failure reason");
        }

        if (transactionStatus == TransactionStatus.COMPLETED && processedAt == null) {
            throw new IllegalStateException("Completed transactions must have a processed date");
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    // Getters
    public Long id() { return id; }
    public Long userSubscriptionId() { return userSubscriptionId; }
    public Long userId() { return userId; }
    public Long paymentMethodId() { return paymentMethodId; }
    public TransactionType transactionType() { return transactionType; }
    public TransactionStatus transactionStatus() { return transactionStatus; }
    public Long fromPlanId() { return fromPlanId; }
    public Long toPlanId() { return toPlanId; }
    public BigDecimal amount() { return amount; }
    public String currency() { return currency; }
    public String stripePaymentIntentId() { return stripePaymentIntentId; }
    public String stripeChargeId() { return stripeChargeId; }
    public String stripeInvoiceId() { return stripeInvoiceId; }
    public String gatewayResponse() { return gatewayResponse; }
    public String failureReason() { return failureReason; }
    public LocalDateTime periodStart() { return periodStart; }
    public LocalDateTime periodEnd() { return periodEnd; }
    public LocalDateTime processedAt() { return processedAt; }
    public LocalDateTime createdAt() { return createdAt; }
    public LocalDateTime updatedAt() { return updatedAt; }

    // Business methods
    public boolean isSuccessful() {
        return transactionStatus.isSuccessful();
    }

    public boolean isPending() {
        return transactionStatus.isInProgress();
    }

    public boolean requiresPayment() {
        return transactionType.requiresPayment();
    }

    public boolean isUpgrade() {
        return transactionType == TransactionType.UPGRADE;
    }

    public boolean isDowngrade() {
        return transactionType == TransactionType.DOWNGRADE;
    }

    public boolean isRefunded() {
        return transactionStatus == TransactionStatus.REFUNDED;
    }

    public SubscriptionTransaction markAsCompleted(String paymentIntentId, String chargeId) {
        return builder()
                .from(this)
                .transactionStatus(TransactionStatus.COMPLETED)
                .stripePaymentIntentId(paymentIntentId)
                .stripeChargeId(chargeId)
                .processedAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    public SubscriptionTransaction markAsFailed(String reason) {
        return builder()
                .from(this)
                .transactionStatus(TransactionStatus.FAILED)
                .failureReason(reason)
                .updatedAt(LocalDateTime.now())
                .build();
    }

    public SubscriptionTransaction markAsRefunded() {
        return builder()
                .from(this)
                .transactionStatus(TransactionStatus.REFUNDED)
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SubscriptionTransaction that = (SubscriptionTransaction) o;
        return Objects.equals(id, that.id) && Objects.equals(stripePaymentIntentId, that.stripePaymentIntentId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, stripePaymentIntentId);
    }

    @Override
    public String toString() {
        return "SubscriptionTransaction{" +
                "id=" + id +
                ", transactionType=" + transactionType +
                ", transactionStatus=" + transactionStatus +
                ", amount=" + amount +
                ", currency='" + currency + '\'' +
                ", processedAt=" + processedAt +
                '}';
    }

    public static final class Builder {
        private Long id;
        private Long userSubscriptionId;
        private Long userId;
        private Long paymentMethodId;
        private TransactionType transactionType;
        private TransactionStatus transactionStatus = TransactionStatus.PENDING;
        private Long fromPlanId;
        private Long toPlanId;
        private BigDecimal amount;
        private String currency = "USD";
        private String stripePaymentIntentId;
        private String stripeChargeId;
        private String stripeInvoiceId;
        private String gatewayResponse;
        private String failureReason;
        private LocalDateTime periodStart;
        private LocalDateTime periodEnd;
        private LocalDateTime processedAt;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        private Builder() {}

        public Builder id(Long id) { this.id = id; return this; }
        public Builder userSubscriptionId(Long userSubscriptionId) { this.userSubscriptionId = userSubscriptionId; return this; }
        public Builder userId(Long userId) { this.userId = userId; return this; }
        public Builder paymentMethodId(Long paymentMethodId) { this.paymentMethodId = paymentMethodId; return this; }
        public Builder transactionType(TransactionType transactionType) { this.transactionType = transactionType; return this; }
        public Builder transactionStatus(TransactionStatus transactionStatus) { this.transactionStatus = transactionStatus; return this; }
        public Builder fromPlanId(Long fromPlanId) { this.fromPlanId = fromPlanId; return this; }
        public Builder toPlanId(Long toPlanId) { this.toPlanId = toPlanId; return this; }
        public Builder amount(BigDecimal amount) { this.amount = amount; return this; }
        public Builder currency(String currency) { this.currency = currency; return this; }
        public Builder stripePaymentIntentId(String stripePaymentIntentId) { this.stripePaymentIntentId = stripePaymentIntentId; return this; }
        public Builder stripeChargeId(String stripeChargeId) { this.stripeChargeId = stripeChargeId; return this; }
        public Builder stripeInvoiceId(String stripeInvoiceId) { this.stripeInvoiceId = stripeInvoiceId; return this; }
        public Builder gatewayResponse(String gatewayResponse) { this.gatewayResponse = gatewayResponse; return this; }
        public Builder failureReason(String failureReason) { this.failureReason = failureReason; return this; }
        public Builder periodStart(LocalDateTime periodStart) { this.periodStart = periodStart; return this; }
        public Builder periodEnd(LocalDateTime periodEnd) { this.periodEnd = periodEnd; return this; }
        public Builder processedAt(LocalDateTime processedAt) { this.processedAt = processedAt; return this; }
        public Builder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public Builder updatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; return this; }

        public Builder from(SubscriptionTransaction transaction) {
            this.id = transaction.id;
            this.userSubscriptionId = transaction.userSubscriptionId;
            this.userId = transaction.userId;
            this.paymentMethodId = transaction.paymentMethodId;
            this.transactionType = transaction.transactionType;
            this.transactionStatus = transaction.transactionStatus;
            this.fromPlanId = transaction.fromPlanId;
            this.toPlanId = transaction.toPlanId;
            this.amount = transaction.amount;
            this.currency = transaction.currency;
            this.stripePaymentIntentId = transaction.stripePaymentIntentId;
            this.stripeChargeId = transaction.stripeChargeId;
            this.stripeInvoiceId = transaction.stripeInvoiceId;
            this.gatewayResponse = transaction.gatewayResponse;
            this.failureReason = transaction.failureReason;
            this.periodStart = transaction.periodStart;
            this.periodEnd = transaction.periodEnd;
            this.processedAt = transaction.processedAt;
            this.createdAt = transaction.createdAt;
            return this;
        }

        public SubscriptionTransaction build() {
            return new SubscriptionTransaction(this);
        }
    }
}
