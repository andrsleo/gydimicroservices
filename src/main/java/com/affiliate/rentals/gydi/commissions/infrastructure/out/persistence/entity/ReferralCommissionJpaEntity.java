package com.affiliate.rentals.gydi.commissions.infrastructure.out.persistence.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "referral_commission", schema = "commissions")
public class ReferralCommissionJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "booking_id", nullable = false, unique = true)
    private Long bookingId;

    @Column(name = "affiliate_id", nullable = false)
    private Long affiliateId;

    @Column(name = "affiliate_plan", nullable = false, length = 50)
    private String affiliatePlan;

    @Column(name = "commission_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal commissionRate;

    @Column(name = "booking_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal bookingAmount;

    @Column(name = "commission_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal commissionAmount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency = "USD";

    @Column(name = "scheduled_payment_date", nullable = false)
    private LocalDate scheduledPaymentDate;

    @Column(name = "dispute_period_ends_at", nullable = false)
    private LocalDateTime disputePeriodEndsAt;

    @Column(name = "paypal_payout_batch_id", length = 255)
    private String paypalPayoutBatchId;

    @Column(name = "paypal_payout_item_id", length = 255)
    private String paypalPayoutItemId;

    @Column(name = "stripe_transfer_id", length = 255)
    private String stripeTransferId;

    @Column(name = "stripe_payout_id", length = 255)
    private String stripePayoutId;

    @Column(name = "status", nullable = false, length = 50)
    private String status = "WAITING_HOST_CHARGE";

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;

    @Column(name = "attempt_count", nullable = false)
    private Integer attemptCount = 0;

    @Column(name = "last_attempt_at")
    private LocalDateTime lastAttemptAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getBookingId() { return bookingId; }
    public void setBookingId(Long bookingId) { this.bookingId = bookingId; }
    public Long getAffiliateId() { return affiliateId; }
    public void setAffiliateId(Long affiliateId) { this.affiliateId = affiliateId; }
    public String getAffiliatePlan() { return affiliatePlan; }
    public void setAffiliatePlan(String affiliatePlan) { this.affiliatePlan = affiliatePlan; }
    public BigDecimal getCommissionRate() { return commissionRate; }
    public void setCommissionRate(BigDecimal commissionRate) { this.commissionRate = commissionRate; }
    public BigDecimal getBookingAmount() { return bookingAmount; }
    public void setBookingAmount(BigDecimal bookingAmount) { this.bookingAmount = bookingAmount; }
    public BigDecimal getCommissionAmount() { return commissionAmount; }
    public void setCommissionAmount(BigDecimal commissionAmount) { this.commissionAmount = commissionAmount; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public LocalDate getScheduledPaymentDate() { return scheduledPaymentDate; }
    public void setScheduledPaymentDate(LocalDate scheduledPaymentDate) { this.scheduledPaymentDate = scheduledPaymentDate; }
    public LocalDateTime getDisputePeriodEndsAt() { return disputePeriodEndsAt; }
    public void setDisputePeriodEndsAt(LocalDateTime disputePeriodEndsAt) { this.disputePeriodEndsAt = disputePeriodEndsAt; }
    public String getPaypalPayoutBatchId() { return paypalPayoutBatchId; }
    public void setPaypalPayoutBatchId(String paypalPayoutBatchId) { this.paypalPayoutBatchId = paypalPayoutBatchId; }
    public String getPaypalPayoutItemId() { return paypalPayoutItemId; }
    public void setPaypalPayoutItemId(String paypalPayoutItemId) { this.paypalPayoutItemId = paypalPayoutItemId; }
    public String getStripeTransferId() { return stripeTransferId; }
    public void setStripeTransferId(String stripeTransferId) { this.stripeTransferId = stripeTransferId; }
    public String getStripePayoutId() { return stripePayoutId; }
    public void setStripePayoutId(String stripePayoutId) { this.stripePayoutId = stripePayoutId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getPaidAt() { return paidAt; }
    public void setPaidAt(LocalDateTime paidAt) { this.paidAt = paidAt; }
    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }
    public Integer getAttemptCount() { return attemptCount; }
    public void setAttemptCount(Integer attemptCount) { this.attemptCount = attemptCount; }
    public LocalDateTime getLastAttemptAt() { return lastAttemptAt; }
    public void setLastAttemptAt(LocalDateTime lastAttemptAt) { this.lastAttemptAt = lastAttemptAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
