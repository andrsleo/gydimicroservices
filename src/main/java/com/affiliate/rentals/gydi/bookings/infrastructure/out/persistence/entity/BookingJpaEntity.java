package com.affiliate.rentals.gydi.bookings.infrastructure.out.persistence.entity;

import com.affiliate.rentals.gydi.bookings.domain.model.BookingStatus;
import com.affiliate.rentals.gydi.shared.encryption.PiiEncryptor;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * JPA Entity for bookings.booking table.
 * <p>
 * Infrastructure concern - separate from domain model.
 * Mapping between BookingJpaEntity and Booking (domain) is done by
 * BookingEntityMapper.
 * </p>
 */
@Entity
@Table(name = "booking", schema = "bookings")
public class BookingJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "referral_link_id", nullable = false)
    private Long referralLinkId;

    @Column(name = "property_id", nullable = false)
    private Long propertyId;

    @Column(name = "check_in_date", nullable = false)
    private LocalDate checkInDate;

    @Column(name = "check_out_date", nullable = false)
    private LocalDate checkOutDate;

    // Guest Information (✅ SECURITY FIX: PII Encryption at Rest - FIX-007)
    @Column(name = "guest_name", nullable = false, columnDefinition = "TEXT")
    @Convert(converter = PiiEncryptor.class)
    private String guestName;

    @Column(name = "guest_email", nullable = false, columnDefinition = "TEXT")
    @Convert(converter = PiiEncryptor.class)
    private String guestEmail;

    @Column(name = "guest_phone", columnDefinition = "TEXT")
    @Convert(converter = PiiEncryptor.class)
    private String guestPhone;

    @Column(name = "guests_count", nullable = false)
    private Integer guestsCount;

    // Airbnb Integration
    @Column(name = "airbnb_confirmation_code", length = 100)
    private String airbnbConfirmationCode;

    // Financial
    @Column(name = "total_amount", precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "currency", length = 3, nullable = false)
    private String currency = "USD";

    // Commission Snapshots (Added in V82)
    @Column(name = "host_commission_rate", precision = 5, scale = 4)
    private BigDecimal hostCommissionRate;

    @Column(name = "affiliate_commission_rate", precision = 5, scale = 4)
    private BigDecimal affiliateCommissionRate;

    // Status
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "status", nullable = false, length = 50)
    private BookingStatus status;

    // Lifecycle Timestamps
    @Column(name = "reserved_at")
    private LocalDateTime reservedAt;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "finished_at")
    private LocalDateTime finishedAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    // Fase 1 — Host rejection fields (V99)
    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    // Fase 2 — Stripe payment fields (V99)
    @Column(name = "stripe_booking_intent_id", length = 100)
    private String stripeBookingIntentId;

    @Column(name = "stripe_deposit_intent_id", length = 100)
    private String stripeDepositIntentId;

    @Column(name = "deposit_amount", precision = 12, scale = 2)
    private BigDecimal depositAmount;

    @Column(name = "deposit_currency", length = 3)
    private String depositCurrency;

    @Column(name = "deposit_captured_at")
    private LocalDateTime depositCapturedAt;

    @Column(name = "deposit_capture_amount", precision = 12, scale = 2)
    private BigDecimal depositCaptureAmount;

    @Column(name = "payment_released_at")
    private LocalDateTime paymentReleasedAt;

    // Audit
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // Relationship with status history (not loaded by default)
    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<BookingStatusHistoryJpaEntity> statusHistory = new ArrayList<>();

    // Default constructor for JPA
    public BookingJpaEntity() {
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getReferralLinkId() {
        return referralLinkId;
    }

    public void setReferralLinkId(Long referralLinkId) {
        this.referralLinkId = referralLinkId;
    }

    public Long getPropertyId() {
        return propertyId;
    }

    public void setPropertyId(Long propertyId) {
        this.propertyId = propertyId;
    }

    public LocalDate getCheckInDate() {
        return checkInDate;
    }

    public void setCheckInDate(LocalDate checkInDate) {
        this.checkInDate = checkInDate;
    }

    public LocalDate getCheckOutDate() {
        return checkOutDate;
    }

    public void setCheckOutDate(LocalDate checkOutDate) {
        this.checkOutDate = checkOutDate;
    }

    public String getGuestName() {
        return guestName;
    }

    public void setGuestName(String guestName) {
        this.guestName = guestName;
    }

    public String getGuestEmail() {
        return guestEmail;
    }

    public void setGuestEmail(String guestEmail) {
        this.guestEmail = guestEmail;
    }

    public String getGuestPhone() {
        return guestPhone;
    }

    public void setGuestPhone(String guestPhone) {
        this.guestPhone = guestPhone;
    }

    public Integer getGuestsCount() {
        return guestsCount;
    }

    public void setGuestsCount(Integer guestsCount) {
        this.guestsCount = guestsCount;
    }

    public String getAirbnbConfirmationCode() {
        return airbnbConfirmationCode;
    }

    public void setAirbnbConfirmationCode(String airbnbConfirmationCode) {
        this.airbnbConfirmationCode = airbnbConfirmationCode;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public BigDecimal getHostCommissionRate() {
        return hostCommissionRate;
    }

    public void setHostCommissionRate(BigDecimal hostCommissionRate) {
        this.hostCommissionRate = hostCommissionRate;
    }

    public BigDecimal getAffiliateCommissionRate() {
        return affiliateCommissionRate;
    }

    public void setAffiliateCommissionRate(BigDecimal affiliateCommissionRate) {
        this.affiliateCommissionRate = affiliateCommissionRate;
    }

    public BookingStatus getStatus() {
        return status;
    }

    public void setStatus(BookingStatus status) {
        this.status = status;
    }

    public List<BookingStatusHistoryJpaEntity> getStatusHistory() {
        return statusHistory;
    }

    public void setStatusHistory(List<BookingStatusHistoryJpaEntity> statusHistory) {
        this.statusHistory = statusHistory;
    }

    public LocalDateTime getReservedAt() {
        return reservedAt;
    }

    public void setReservedAt(LocalDateTime reservedAt) {
        this.reservedAt = reservedAt;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public LocalDateTime getFinishedAt() {
        return finishedAt;
    }

    public void setFinishedAt(LocalDateTime finishedAt) {
        this.finishedAt = finishedAt;
    }

    public LocalDateTime getCancelledAt() {
        return cancelledAt;
    }

    public void setCancelledAt(LocalDateTime cancelledAt) {
        this.cancelledAt = cancelledAt;
    }

    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }

    public String getStripeBookingIntentId() { return stripeBookingIntentId; }
    public void setStripeBookingIntentId(String stripeBookingIntentId) { this.stripeBookingIntentId = stripeBookingIntentId; }

    public String getStripeDepositIntentId() { return stripeDepositIntentId; }
    public void setStripeDepositIntentId(String stripeDepositIntentId) { this.stripeDepositIntentId = stripeDepositIntentId; }

    public BigDecimal getDepositAmount() { return depositAmount; }
    public void setDepositAmount(BigDecimal depositAmount) { this.depositAmount = depositAmount; }

    public String getDepositCurrency() { return depositCurrency; }
    public void setDepositCurrency(String depositCurrency) { this.depositCurrency = depositCurrency; }

    public LocalDateTime getDepositCapturedAt() { return depositCapturedAt; }
    public void setDepositCapturedAt(LocalDateTime depositCapturedAt) { this.depositCapturedAt = depositCapturedAt; }

    public BigDecimal getDepositCaptureAmount() { return depositCaptureAmount; }
    public void setDepositCaptureAmount(BigDecimal depositCaptureAmount) { this.depositCaptureAmount = depositCaptureAmount; }

    public LocalDateTime getPaymentReleasedAt() { return paymentReleasedAt; }
    public void setPaymentReleasedAt(LocalDateTime paymentReleasedAt) { this.paymentReleasedAt = paymentReleasedAt; }

    // Phase 4 — Social Commerce
    @Column(name = "content_post_id")
    private Long contentPostId;

    public Long getContentPostId() { return contentPostId; }
    public void setContentPostId(Long contentPostId) { this.contentPostId = contentPostId; }

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
        BookingJpaEntity that = (BookingJpaEntity) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return String.format("BookingJpaEntity{id=%d, property=%d, status=%s}",
                id, propertyId, status);
    }
}
