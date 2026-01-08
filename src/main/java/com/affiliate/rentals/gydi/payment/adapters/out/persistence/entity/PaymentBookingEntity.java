package com.affiliate.rentals.gydi.payment.adapters.out.persistence.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * JPA Entity for payment.booking table.
 * <p>
 * Represents a payment record for a finished booking with commission.
 * Separate from domain model - mapping handled by PaymentRepositoryAdapter.
 * </p>
 */
@Entity
@Table(name = "payment_booking", schema = "payment")
public class PaymentBookingEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "booking_id", nullable = false, unique = true)
    private Long bookingId;

    @Column(name = "referral_link_id", nullable = false)
    private Long referralLinkId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "property_id", nullable = false)
    private Long propertyId;

    @Column(name = "total_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "percentage_commission", nullable = false, precision = 5, scale = 2)
    private BigDecimal commissionPercentage;

    @Column(name = "commission_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal commissionAmount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false, length = 20)
    private PaymentStatusEntity status;

    @Column(name = "payment_method", length = 50)
    private String paymentMethod;

    @Column(name = "transaction_id", length = 255)
    private String transactionId;

    @Column(name = "gateway_name", length = 100)
    private String gatewayName;

    @Column(name = "gateway_response", columnDefinition = "TEXT")
    private String gatewayResponse;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // Default constructor for JPA
    public PaymentBookingEntity() {
    }

    // Builder-style setters
    public static PaymentBookingEntity create() {
        return new PaymentBookingEntity();
    }

    public PaymentBookingEntity id(Long id) {
        this.id = id;
        return this;
    }

    public PaymentBookingEntity bookingId(Long bookingId) {
        this.bookingId = bookingId;
        return this;
    }

    public PaymentBookingEntity referralLinkId(Long referralLinkId) {
        this.referralLinkId = referralLinkId;
        return this;
    }

    public PaymentBookingEntity userId(Long userId) {
        this.userId = userId;
        return this;
    }

    public PaymentBookingEntity propertyId(Long propertyId) {
        this.propertyId = propertyId;
        return this;
    }

    public PaymentBookingEntity totalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
        return this;
    }

    public PaymentBookingEntity commissionPercentage(BigDecimal commissionPercentage) {
        this.commissionPercentage = commissionPercentage;
        return this;
    }

    public PaymentBookingEntity commissionAmount(BigDecimal commissionAmount) {
        this.commissionAmount = commissionAmount;
        return this;
    }

    public PaymentBookingEntity currency(String currency) {
        this.currency = currency;
        return this;
    }

    public PaymentBookingEntity status(PaymentStatusEntity status) {
        this.status = status;
        return this;
    }

    public PaymentBookingEntity paymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
        return this;
    }

    public PaymentBookingEntity transactionId(String transactionId) {
        this.transactionId = transactionId;
        return this;
    }

    public PaymentBookingEntity gatewayName(String gatewayName) {
        this.gatewayName = gatewayName;
        return this;
    }

    public PaymentBookingEntity gatewayResponse(String gatewayResponse) {
        this.gatewayResponse = gatewayResponse;
        return this;
    }

    public PaymentBookingEntity paidAt(LocalDateTime paidAt) {
        this.paidAt = paidAt;
        return this;
    }

    public PaymentBookingEntity failureReason(String failureReason) {
        this.failureReason = failureReason;
        return this;
    }

    // Getters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getBookingId() {
        return bookingId;
    }

    public void setBookingId(Long bookingId) {
        this.bookingId = bookingId;
    }

    public Long getReferralLinkId() {
        return referralLinkId;
    }

    public void setReferralLinkId(Long referralLinkId) {
        this.referralLinkId = referralLinkId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getPropertyId() {
        return propertyId;
    }

    public void setPropertyId(Long propertyId) {
        this.propertyId = propertyId;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public BigDecimal getCommissionPercentage() {
        return commissionPercentage;
    }

    public void setCommissionPercentage(BigDecimal commissionPercentage) {
        this.commissionPercentage = commissionPercentage;
    }

    public BigDecimal getCommissionAmount() {
        return commissionAmount;
    }

    public void setCommissionAmount(BigDecimal commissionAmount) {
        this.commissionAmount = commissionAmount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public PaymentStatusEntity getStatus() {
        return status;
    }

    public void setStatus(PaymentStatusEntity status) {
        this.status = status;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public String getGatewayName() {
        return gatewayName;
    }

    public void setGatewayName(String gatewayName) {
        this.gatewayName = gatewayName;
    }

    public String getGatewayResponse() {
        return gatewayResponse;
    }

    public void setGatewayResponse(String gatewayResponse) {
        this.gatewayResponse = gatewayResponse;
    }

    public LocalDateTime getPaidAt() {
        return paidAt;
    }

    public void setPaidAt(LocalDateTime paidAt) {
        this.paidAt = paidAt;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        PaymentBookingEntity that = (PaymentBookingEntity) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "PaymentBookingEntity{" +
                "id=" + id +
                ", bookingId=" + bookingId +
                ", userId=" + userId +
                ", commissionAmount=" + commissionAmount +
                ", status=" + status +
                '}';
    }

    /**
     * JPA enum that matches database enum payment.payment_status.
     */
    public enum PaymentStatusEntity {
        PENDING,
        PROCESSING,
        SUCCESS,
        FAILED,
        CANCELED
    }
}
