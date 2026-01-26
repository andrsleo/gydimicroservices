package com.affiliate.rentals.gydi.bookings.adapters.out.persistence.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * JPA Entity for referrals.booking table.
 * <p>
 * This is an infrastructure concern, separate from the domain model.
 * Mapping between BookingEntity and Booking (domain) is done by
 * BookingRepositoryAdapter.
 * </p>
 */
@Entity
@Table(name = "booking", schema = "referrals")
public class BookingEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "booking_id")
    private Long id;

    @Column(name = "referral_link_id", nullable = false)
    private Long referralLinkId;

    @Column(name = "property_id", nullable = false)
    private Long propertyId;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "client_email", nullable = false, length = 255)
    private String clientEmail;

    @Column(name = "client_phone", length = 20)
    private String clientPhone;

    @Column(name = "client_first_name", nullable = false, length = 100)
    private String clientFirstName;

    @Column(name = "client_last_name", nullable = false, length = 100)
    private String clientLastName;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)  // Fix PostgreSQL ENUM casting error
    @Column(name = "status", nullable = false, length = 50)
    private BookingStatusEntity status;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // Default constructor for JPA
    public BookingEntity() {
    }

    // Builder-style setters for convenience
    public static BookingEntity create() {
        return new BookingEntity();
    }

    public BookingEntity id(Long id) {
        this.id = id;
        return this;
    }

    public BookingEntity referralLinkId(Long referralLinkId) {
        this.referralLinkId = referralLinkId;
        return this;
    }

    public BookingEntity propertyId(Long propertyId) {
        this.propertyId = propertyId;
        return this;
    }

    public BookingEntity startDate(LocalDate startDate) {
        this.startDate = startDate;
        return this;
    }

    public BookingEntity endDate(LocalDate endDate) {
        this.endDate = endDate;
        return this;
    }

    public BookingEntity clientEmail(String clientEmail) {
        this.clientEmail = clientEmail;
        return this;
    }

    public BookingEntity clientPhone(String clientPhone) {
        this.clientPhone = clientPhone;
        return this;
    }

    public BookingEntity clientFirstName(String clientFirstName) {
        this.clientFirstName = clientFirstName;
        return this;
    }

    public BookingEntity clientLastName(String clientLastName) {
        this.clientLastName = clientLastName;
        return this;
    }

    public BookingEntity status(BookingStatusEntity status) {
        this.status = status;
        return this;
    }

    // Getters
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

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public String getClientEmail() {
        return clientEmail;
    }

    public void setClientEmail(String clientEmail) {
        this.clientEmail = clientEmail;
    }

    public String getClientPhone() {
        return clientPhone;
    }

    public void setClientPhone(String clientPhone) {
        this.clientPhone = clientPhone;
    }

    public String getClientFirstName() {
        return clientFirstName;
    }

    public void setClientFirstName(String clientFirstName) {
        this.clientFirstName = clientFirstName;
    }

    public String getClientLastName() {
        return clientLastName;
    }

    public void setClientLastName(String clientLastName) {
        this.clientLastName = clientLastName;
    }

    public BookingStatusEntity getStatus() {
        return status;
    }

    public void setStatus(BookingStatusEntity status) {
        this.status = status;
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
        BookingEntity that = (BookingEntity) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "BookingEntity{" +
                "id=" + id +
                ", propertyId=" + propertyId +
                ", startDate=" + startDate +
                ", endDate=" + endDate +
                ", status=" + status +
                '}';
    }

    /**
     * JPA enum that matches database enum referrals.booking_status.
     */
    public enum BookingStatusEntity {
        REQUEST,
        RESERVED,
        FINISHED,
        CANCELED
    }
}
