package com.affiliate.rentals.gydi.bookings.infrastructure.out.persistence.entity;

import com.affiliate.rentals.gydi.bookings.domain.model.PropertyCalendar.BlockReason;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Objects;

/**
 * JPA Entity for bookings.property_calendar table.
 * <p>
 * Infrastructure concern — separate from domain model PropertyCalendar.
 * </p>
 */
@Entity
@Table(name = "property_calendar", schema = "bookings")
public class PropertyCalendarJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "property_id", nullable = false)
    private Long propertyId;

    @Column(name = "blocked_date", nullable = false)
    private LocalDate blockedDate;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "block_reason", nullable = false, length = 50)
    private BlockReason blockReason;

    @Column(name = "booking_id")
    private Long bookingId;

    @Column(name = "created_by_host_id", nullable = false)
    private Long createdByHostId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    // Default constructor for JPA
    public PropertyCalendarJpaEntity() {
    }

    // Getters and Setters

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getPropertyId() { return propertyId; }
    public void setPropertyId(Long propertyId) { this.propertyId = propertyId; }

    public LocalDate getBlockedDate() { return blockedDate; }
    public void setBlockedDate(LocalDate blockedDate) { this.blockedDate = blockedDate; }

    public BlockReason getBlockReason() { return blockReason; }
    public void setBlockReason(BlockReason blockReason) { this.blockReason = blockReason; }

    public Long getBookingId() { return bookingId; }
    public void setBookingId(Long bookingId) { this.bookingId = bookingId; }

    public Long getCreatedByHostId() { return createdByHostId; }
    public void setCreatedByHostId(Long createdByHostId) { this.createdByHostId = createdByHostId; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PropertyCalendarJpaEntity that = (PropertyCalendarJpaEntity) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return String.format("PropertyCalendarJpaEntity{id=%d, property=%d, date=%s, reason=%s}",
                id, propertyId, blockedDate, blockReason);
    }
}
