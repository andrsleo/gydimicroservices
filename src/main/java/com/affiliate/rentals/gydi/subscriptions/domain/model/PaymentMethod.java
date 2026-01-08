package com.affiliate.rentals.gydi.subscriptions.domain.model;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Domain model representing a user's payment method.
 *
 * <p>
 * This immutable entity stores tokenized payment information from the payment
 * gateway
 * (e.g., Stripe). It NEVER stores raw card numbers for PCI compliance.
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
 * @author GYDI Development Team
 */
public final class PaymentMethod {
    private final Long id;
    private final Long userId;
    private final PaymentMethodType methodType;
    private final String gatewayProvider;
    private final String gatewayToken;
    private final String cardLastFour;
    private final String cardBrand;
    private final Integer cardExpMonth;
    private final Integer cardExpYear;
    private final String billingEmail;
    private final boolean isDefault;
    private final boolean isActive;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    private PaymentMethod(Builder builder) {
        this.id = builder.id;
        this.userId = Objects.requireNonNull(builder.userId, "User ID cannot be null");
        this.methodType = Objects.requireNonNull(builder.methodType, "Payment method type cannot be null");
        this.gatewayProvider = Objects.requireNonNull(builder.gatewayProvider, "Gateway provider cannot be null");
        this.gatewayToken = Objects.requireNonNull(builder.gatewayToken, "Gateway token cannot be null");
        this.cardLastFour = builder.cardLastFour;
        this.cardBrand = builder.cardBrand;
        this.cardExpMonth = builder.cardExpMonth;
        this.cardExpYear = builder.cardExpYear;
        this.billingEmail = builder.billingEmail;
        this.isDefault = builder.isDefault;
        this.isActive = builder.isActive;
        this.createdAt = Objects.requireNonNullElseGet(builder.createdAt, LocalDateTime::now);
        this.updatedAt = builder.updatedAt;

        validateBusinessRules();
    }

    private void validateBusinessRules() {
        if (methodType.isCard()) {
            if (cardLastFour == null || cardLastFour.length() != 4) {
                throw new IllegalArgumentException("Card last four digits must be exactly 4 digits");
            }
            if (cardExpMonth == null || cardExpMonth < 1 || cardExpMonth > 12) {
                throw new IllegalArgumentException("Card expiration month must be between 1 and 12");
            }
            if (cardExpYear == null || cardExpYear < LocalDateTime.now().getYear()) {
                throw new IllegalArgumentException("Card expiration year cannot be in the past");
            }
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    // Getters
    public Long id() {
        return id;
    }

    public Long userId() {
        return userId;
    }

    public PaymentMethodType methodType() {
        return methodType;
    }

    public String gatewayProvider() {
        return gatewayProvider;
    }

    public String gatewayToken() {
        return gatewayToken;
    }

    public String cardLastFour() {
        return cardLastFour;
    }

    public String cardBrand() {
        return cardBrand;
    }

    public Integer cardExpMonth() {
        return cardExpMonth;
    }

    public Integer cardExpYear() {
        return cardExpYear;
    }

    public String billingEmail() {
        return billingEmail;
    }

    public boolean isDefault() {
        return isDefault;
    }

    public boolean isActive() {
        return isActive;
    }

    public LocalDateTime createdAt() {
        return createdAt;
    }

    public LocalDateTime updatedAt() {
        return updatedAt;
    }

    // Business methods
    public boolean isExpired() {
        if (cardExpMonth == null || cardExpYear == null)
            return false;

        LocalDateTime now = LocalDateTime.now();
        int currentYear = now.getYear();
        int currentMonth = now.getMonthValue();

        return cardExpYear < currentYear ||
                (cardExpYear == currentYear && cardExpMonth < currentMonth);
    }

    public boolean willExpireSoon(int monthsThreshold) {
        if (cardExpMonth == null || cardExpYear == null)
            return false;

        LocalDateTime expirationDate = LocalDateTime.of(cardExpYear, cardExpMonth, 1, 0, 0);
        LocalDateTime thresholdDate = LocalDateTime.now().plusMonths(monthsThreshold);

        return expirationDate.isBefore(thresholdDate);
    }

    public String getMaskedCardNumber() {
        if (cardLastFour == null)
            return "****";
        return "**** **** **** " + cardLastFour;
    }

    public String getDisplayName() {
        if (methodType.isCard() && cardBrand != null) {
            return cardBrand + " ending in " + cardLastFour;
        }
        return methodType.getDisplayName();
    }

    public PaymentMethod makeDefault() {
        return builder()
                .from(this)
                .isDefault(true)
                .updatedAt(LocalDateTime.now())
                .build();
    }

    public PaymentMethod deactivate() {
        return builder()
                .from(this)
                .isActive(false)
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        PaymentMethod that = (PaymentMethod) o;
        return Objects.equals(id, that.id) && Objects.equals(gatewayToken, that.gatewayToken);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, gatewayToken);
    }

    @Override
    public String toString() {
        return "PaymentMethod{" +
                "id=" + id +
                ", userId=" + userId +
                ", methodType=" + methodType +
                ", gatewayProvider='" + gatewayProvider + '\'' +
                ", cardLastFour='" + cardLastFour + '\'' +
                ", isDefault=" + isDefault +
                ", isActive=" + isActive +
                '}';
    }

    public static final class Builder {
        private Long id;
        private Long userId;
        private PaymentMethodType methodType;
        private String gatewayProvider;
        private String gatewayToken;
        private String cardLastFour;
        private String cardBrand;
        private Integer cardExpMonth;
        private Integer cardExpYear;
        private String billingEmail;
        private boolean isDefault = false;
        private boolean isActive = true;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        private Builder() {
        }

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder userId(Long userId) {
            this.userId = userId;
            return this;
        }

        public Builder methodType(PaymentMethodType methodType) {
            this.methodType = methodType;
            return this;
        }

        public Builder gatewayProvider(String gatewayProvider) {
            this.gatewayProvider = gatewayProvider;
            return this;
        }

        public Builder gatewayToken(String gatewayToken) {
            this.gatewayToken = gatewayToken;
            return this;
        }

        public Builder cardLastFour(String cardLastFour) {
            this.cardLastFour = cardLastFour;
            return this;
        }

        public Builder cardBrand(String cardBrand) {
            this.cardBrand = cardBrand;
            return this;
        }

        public Builder cardExpMonth(Integer cardExpMonth) {
            this.cardExpMonth = cardExpMonth;
            return this;
        }

        public Builder cardExpYear(Integer cardExpYear) {
            this.cardExpYear = cardExpYear;
            return this;
        }

        public Builder billingEmail(String billingEmail) {
            this.billingEmail = billingEmail;
            return this;
        }

        public Builder isDefault(boolean isDefault) {
            this.isDefault = isDefault;
            return this;
        }

        public Builder isActive(boolean isActive) {
            this.isActive = isActive;
            return this;
        }

        public Builder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder updatedAt(LocalDateTime updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public Builder from(PaymentMethod paymentMethod) {
            this.id = paymentMethod.id;
            this.userId = paymentMethod.userId;
            this.methodType = paymentMethod.methodType;
            this.gatewayProvider = paymentMethod.gatewayProvider;
            this.gatewayToken = paymentMethod.gatewayToken;
            this.cardLastFour = paymentMethod.cardLastFour;
            this.cardBrand = paymentMethod.cardBrand;
            this.cardExpMonth = paymentMethod.cardExpMonth;
            this.cardExpYear = paymentMethod.cardExpYear;
            this.billingEmail = paymentMethod.billingEmail;
            this.isDefault = paymentMethod.isDefault;
            this.isActive = paymentMethod.isActive;
            this.createdAt = paymentMethod.createdAt;
            return this;
        }

        public PaymentMethod build() {
            return new PaymentMethod(this);
        }
    }
}
