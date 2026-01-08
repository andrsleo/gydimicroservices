package com.affiliate.rentals.gydi.subscriptions.domain.model;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Domain model representing a user's subscription.
 *
 * <p>This immutable entity represents the relationship between a user and their
 * active subscription plan, including status, payment method, and lifecycle dates.</p>
 *
 * @author GYDI Development Team
 */
public final class UserSubscription {
    private final Long id;
    private final Long userId;
    private final Long planId;
    private final SubscriptionStatus status;
    private final LocalDateTime startedAt;
    private final LocalDateTime expiresAt;
    private final LocalDateTime canceledAt;
    private final String cancelationReason;
    private final Long paymentMethodId;
    private final boolean autoRenew;
    private final LocalDateTime lastRenewalAt;
    private final LocalDateTime nextBillingDate;
    private final String stripeSubscriptionId;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    private UserSubscription(Builder builder) {
        this.id = builder.id;
        this.userId = Objects.requireNonNull(builder.userId, "User ID cannot be null");
        this.planId = Objects.requireNonNull(builder.planId, "Plan ID cannot be null");
        this.status = Objects.requireNonNull(builder.status, "Status cannot be null");
        this.startedAt = Objects.requireNonNullElseGet(builder.startedAt, LocalDateTime::now);
        this.expiresAt = builder.expiresAt;
        this.canceledAt = builder.canceledAt;
        this.cancelationReason = builder.cancelationReason;
        this.paymentMethodId = builder.paymentMethodId;
        this.autoRenew = builder.autoRenew;
        this.lastRenewalAt = builder.lastRenewalAt;
        this.nextBillingDate = builder.nextBillingDate;
        this.stripeSubscriptionId = builder.stripeSubscriptionId;
        this.createdAt = Objects.requireNonNullElseGet(builder.createdAt, LocalDateTime::now);
        this.updatedAt = builder.updatedAt;

        validateBusinessRules();
    }

    private void validateBusinessRules() {
        if (status == SubscriptionStatus.CANCELED && canceledAt == null) {
            throw new IllegalStateException("Canceled subscriptions must have a cancelation date");
        }
        if (canceledAt != null && cancelationReason == null) {
            throw new IllegalStateException("Canceled subscriptions must have a cancelation reason");
        }
        if (expiresAt != null && expiresAt.isBefore(startedAt)) {
            throw new IllegalArgumentException("Expiration date cannot be before start date");
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    // Getters
    public Long id() { return id; }
    public Long userId() { return userId; }
    public Long planId() { return planId; }
    public SubscriptionStatus status() { return status; }
    public LocalDateTime startedAt() { return startedAt; }
    public LocalDateTime expiresAt() { return expiresAt; }
    public LocalDateTime canceledAt() { return canceledAt; }
    public String cancelationReason() { return cancelationReason; }
    public Long paymentMethodId() { return paymentMethodId; }
    public boolean autoRenew() { return autoRenew; }
    public LocalDateTime lastRenewalAt() { return lastRenewalAt; }
    public LocalDateTime nextBillingDate() { return nextBillingDate; }
    public String stripeSubscriptionId() { return stripeSubscriptionId; }
    public LocalDateTime createdAt() { return createdAt; }
    public LocalDateTime updatedAt() { return updatedAt; }

    // Business methods
    public boolean isActive() {
        return status == SubscriptionStatus.ACTIVE;
    }

    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }

    public boolean willExpireSoon(int daysThreshold) {
        if (expiresAt == null) return false;
        return LocalDateTime.now().plusDays(daysThreshold).isAfter(expiresAt);
    }

    public boolean canBeRenewed() {
        return status.canBeRenewed();
    }

    public boolean requiresPaymentMethod() {
        return autoRenew && paymentMethodId == null;
    }

    public UserSubscription withStatus(SubscriptionStatus newStatus) {
        return builder()
                .from(this)
                .status(newStatus)
                .updatedAt(LocalDateTime.now())
                .build();
    }

    public UserSubscription withCancelation(String reason) {
        return builder()
                .from(this)
                .status(SubscriptionStatus.CANCELED)
                .canceledAt(LocalDateTime.now())
                .cancelationReason(reason)
                .autoRenew(false)
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserSubscription that = (UserSubscription) o;
        return Objects.equals(id, that.id) && Objects.equals(userId, that.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, userId);
    }

    @Override
    public String toString() {
        return "UserSubscription{" +
                "id=" + id +
                ", userId=" + userId +
                ", planId=" + planId +
                ", status=" + status +
                ", expiresAt=" + expiresAt +
                '}';
    }

    public static final class Builder {
        private Long id;
        private Long userId;
        private Long planId;
        private SubscriptionStatus status = SubscriptionStatus.ACTIVE;
        private LocalDateTime startedAt;
        private LocalDateTime expiresAt;
        private LocalDateTime canceledAt;
        private String cancelationReason;
        private Long paymentMethodId;
        private boolean autoRenew = true;
        private LocalDateTime lastRenewalAt;
        private LocalDateTime nextBillingDate;
        private String stripeSubscriptionId;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        private Builder() {}

        public Builder id(Long id) { this.id = id; return this; }
        public Builder userId(Long userId) { this.userId = userId; return this; }
        public Builder planId(Long planId) { this.planId = planId; return this; }
        public Builder status(SubscriptionStatus status) { this.status = status; return this; }
        public Builder startedAt(LocalDateTime startedAt) { this.startedAt = startedAt; return this; }
        public Builder expiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; return this; }
        public Builder canceledAt(LocalDateTime canceledAt) { this.canceledAt = canceledAt; return this; }
        public Builder cancelationReason(String cancelationReason) { this.cancelationReason = cancelationReason; return this; }
        public Builder paymentMethodId(Long paymentMethodId) { this.paymentMethodId = paymentMethodId; return this; }
        public Builder autoRenew(boolean autoRenew) { this.autoRenew = autoRenew; return this; }
        public Builder lastRenewalAt(LocalDateTime lastRenewalAt) { this.lastRenewalAt = lastRenewalAt; return this; }
        public Builder nextBillingDate(LocalDateTime nextBillingDate) { this.nextBillingDate = nextBillingDate; return this; }
        public Builder stripeSubscriptionId(String stripeSubscriptionId) { this.stripeSubscriptionId = stripeSubscriptionId; return this; }
        public Builder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public Builder updatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; return this; }

        public Builder from(UserSubscription subscription) {
            this.id = subscription.id;
            this.userId = subscription.userId;
            this.planId = subscription.planId;
            this.status = subscription.status;
            this.startedAt = subscription.startedAt;
            this.expiresAt = subscription.expiresAt;
            this.canceledAt = subscription.canceledAt;
            this.cancelationReason = subscription.cancelationReason;
            this.paymentMethodId = subscription.paymentMethodId;
            this.autoRenew = subscription.autoRenew;
            this.lastRenewalAt = subscription.lastRenewalAt;
            this.nextBillingDate = subscription.nextBillingDate;
            this.stripeSubscriptionId = subscription.stripeSubscriptionId;
            this.createdAt = subscription.createdAt;
            return this;
        }

        public UserSubscription build() {
            return new UserSubscription(this);
        }
    }
}
