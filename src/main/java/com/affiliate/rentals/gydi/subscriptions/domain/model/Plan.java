package com.affiliate.rentals.gydi.subscriptions.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Domain model representing a subscription plan.
 *
 * <p>This immutable entity represents a subscription tier that defines pricing,
 * commission rates, and feature limits for users.</p>
 *
 * <p>Plans are typically defined at the database level and loaded into the application.
 * Examples include FREE, PRO, and ELITE tiers.</p>
 *
 * @author GYDI Development Team
 */
public final class Plan {
    private final Long id;
    private final String planCode;
    private final String planName;
    private final String description;
    private final BigDecimal monthlyPrice;
    private final String currency;
    private final BigDecimal commissionRate;
    private final Integer referralLimitPerMonth;
    private final Integer propertyPublishLimit;
    private final boolean isActive;
    private final boolean isFeatured;
    private final Integer displayOrder;
    private final String stripePriceId;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    private Plan(Builder builder) {
        this.id = builder.id;
        this.planCode = Objects.requireNonNull(builder.planCode, "Plan code cannot be null");
        this.planName = Objects.requireNonNull(builder.planName, "Plan name cannot be null");
        this.description = builder.description;
        this.monthlyPrice = Objects.requireNonNull(builder.monthlyPrice, "Monthly price cannot be null");
        this.currency = Objects.requireNonNullElse(builder.currency, "USD");
        this.commissionRate = Objects.requireNonNull(builder.commissionRate, "Commission rate cannot be null");
        this.referralLimitPerMonth = Objects.requireNonNull(builder.referralLimitPerMonth, "Referral limit cannot be null");
        this.propertyPublishLimit = Objects.requireNonNull(builder.propertyPublishLimit, "Property limit cannot be null");
        this.isActive = builder.isActive;
        this.isFeatured = builder.isFeatured;
        this.displayOrder = builder.displayOrder;
        this.stripePriceId = builder.stripePriceId;
        this.createdAt = Objects.requireNonNullElseGet(builder.createdAt, LocalDateTime::now);
        this.updatedAt = builder.updatedAt;

        validateBusinessRules();
    }

    private void validateBusinessRules() {
        if (monthlyPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Monthly price cannot be negative");
        }
        if (commissionRate.compareTo(BigDecimal.ZERO) < 0 || commissionRate.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException("Commission rate must be between 0 and 1");
        }
        if (referralLimitPerMonth < 0) {
            throw new IllegalArgumentException("Referral limit cannot be negative");
        }
        if (propertyPublishLimit < 0) {
            throw new IllegalArgumentException("Property limit cannot be negative");
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    // Getters
    public Long id() { return id; }
    public String planCode() { return planCode; }
    public String planName() { return planName; }
    public String description() { return description; }
    public BigDecimal monthlyPrice() { return monthlyPrice; }
    public String currency() { return currency; }
    public BigDecimal commissionRate() { return commissionRate; }
    public Integer referralLimitPerMonth() { return referralLimitPerMonth; }
    public Integer propertyPublishLimit() { return propertyPublishLimit; }
    public boolean isActive() { return isActive; }
    public boolean isFeatured() { return isFeatured; }
    public Integer displayOrder() { return displayOrder; }
    public String stripePriceId() { return stripePriceId; }
    public LocalDateTime createdAt() { return createdAt; }
    public LocalDateTime updatedAt() { return updatedAt; }

    // Business methods
    public boolean isFree() {
        return monthlyPrice.compareTo(BigDecimal.ZERO) == 0;
    }

    public boolean hasUnlimitedReferrals() {
        return referralLimitPerMonth == Integer.MAX_VALUE;
    }

    public boolean hasUnlimitedProperties() {
        return propertyPublishLimit == Integer.MAX_VALUE;
    }

    public BigDecimal calculateCommission(BigDecimal amount) {
        return amount.multiply(commissionRate);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Plan plan = (Plan) o;
        return Objects.equals(id, plan.id) && Objects.equals(planCode, plan.planCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, planCode);
    }

    @Override
    public String toString() {
        return "Plan{" +
                "id=" + id +
                ", planCode='" + planCode + '\'' +
                ", planName='" + planName + '\'' +
                ", monthlyPrice=" + monthlyPrice +
                ", currency='" + currency + '\'' +
                '}';
    }

    public static final class Builder {
        private Long id;
        private String planCode;
        private String planName;
        private String description;
        private BigDecimal monthlyPrice;
        private String currency = "USD";
        private BigDecimal commissionRate;
        private Integer referralLimitPerMonth;
        private Integer propertyPublishLimit;
        private boolean isActive = true;
        private boolean isFeatured = false;
        private Integer displayOrder;
        private String stripePriceId;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        private Builder() {}

        public Builder id(Long id) { this.id = id; return this; }
        public Builder planCode(String planCode) { this.planCode = planCode; return this; }
        public Builder planName(String planName) { this.planName = planName; return this; }
        public Builder description(String description) { this.description = description; return this; }
        public Builder monthlyPrice(BigDecimal monthlyPrice) { this.monthlyPrice = monthlyPrice; return this; }
        public Builder currency(String currency) { this.currency = currency; return this; }
        public Builder commissionRate(BigDecimal commissionRate) { this.commissionRate = commissionRate; return this; }
        public Builder referralLimitPerMonth(Integer referralLimitPerMonth) { this.referralLimitPerMonth = referralLimitPerMonth; return this; }
        public Builder propertyPublishLimit(Integer propertyPublishLimit) { this.propertyPublishLimit = propertyPublishLimit; return this; }
        public Builder isActive(boolean isActive) { this.isActive = isActive; return this; }
        public Builder isFeatured(boolean isFeatured) { this.isFeatured = isFeatured; return this; }
        public Builder displayOrder(Integer displayOrder) { this.displayOrder = displayOrder; return this; }
        public Builder stripePriceId(String stripePriceId) { this.stripePriceId = stripePriceId; return this; }
        public Builder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public Builder updatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; return this; }

        public Plan build() {
            return new Plan(this);
        }
    }
}
