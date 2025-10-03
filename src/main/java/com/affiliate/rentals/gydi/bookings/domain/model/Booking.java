package com.affiliate.rentals.gydi.bookings.domain.model;

import com.affiliate.rentals.gydi.properties.domain.model.Money;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Booking domain entity - represents a property booking/reservation.
 * Immutable by design following clean code and SOLID principles.
 * Uses value objects for better domain modeling.
 */
public final class Booking {
    private final Long id;
    private final Long propertyId;
    private final Long userId;
    private final DateRange dateRange;
    private final Money totalPrice;
    private final BookingStatus status;
    private final LocalDateTime createdAt;

    private Booking(Builder builder) {
        this.id = builder.id;
        this.propertyId = Objects.requireNonNull(builder.propertyId, "Property ID cannot be null");
        this.userId = Objects.requireNonNull(builder.userId, "User ID cannot be null");
        this.dateRange = Objects.requireNonNull(builder.dateRange, "Date range cannot be null");
        this.totalPrice = Objects.requireNonNull(builder.totalPrice, "Total price cannot be null");
        this.status = Objects.requireNonNullElse(builder.status, BookingStatus.PENDING);
        this.createdAt = Objects.requireNonNullElseGet(builder.createdAt, LocalDateTime::now);

        validate();
    }

    private void validate() {
        if (totalPrice.isZero()) {
            throw new IllegalArgumentException("Total price must be greater than zero");
        }
    }

    public Long id() {
        return id;
    }

    public Long propertyId() {
        return propertyId;
    }

    public Long userId() {
        return userId;
    }

    public DateRange dateRange() {
        return dateRange;
    }

    public LocalDate startDate() {
        return dateRange.startDate();
    }

    public LocalDate endDate() {
        return dateRange.endDate();
    }

    public Money totalPrice() {
        return totalPrice;
    }

    public BookingStatus status() {
        return status;
    }

    public LocalDateTime createdAt() {
        return createdAt;
    }

    public long numberOfNights() {
        return dateRange.numberOfNights();
    }

    public boolean isActive() {
        return status.isActive();
    }

    public boolean isPending() {
        return status == BookingStatus.PENDING;
    }

    public boolean isConfirmed() {
        return status == BookingStatus.CONFIRMED;
    }

    public boolean isCancelled() {
        return status == BookingStatus.CANCELLED;
    }

    public boolean isCompleted() {
        return status == BookingStatus.COMPLETED;
    }

    public boolean overlapsWith(Booking other) {
        return this.dateRange.overlaps(other.dateRange);
    }

    public boolean isForProperty(Long propertyId) {
        return this.propertyId.equals(propertyId);
    }

    public boolean isForUser(Long userId) {
        return this.userId.equals(userId);
    }

    public Booking confirm() {
        return changeStatus(BookingStatus.CONFIRMED);
    }

    public Booking cancel() {
        return changeStatus(BookingStatus.CANCELLED);
    }

    public Booking complete() {
        return changeStatus(BookingStatus.COMPLETED);
    }

    public Booking startProgress() {
        return changeStatus(BookingStatus.IN_PROGRESS);
    }

    public Booking changeStatus(BookingStatus newStatus) {
        if (!this.status.canTransitionTo(newStatus)) {
            throw new IllegalStateException(
                    "Cannot transition from %s to %s".formatted(this.status, newStatus)
            );
        }
        return this.toBuilder().status(newStatus).build();
    }

    public Money pricePerNight() {
        return totalPrice.divide(numberOfNights());
    }

    public static Builder builder() {
        return new Builder();
    }

    public Builder toBuilder() {
        return new Builder()
                .id(this.id)
                .propertyId(this.propertyId)
                .userId(this.userId)
                .dateRange(this.dateRange)
                .totalPrice(this.totalPrice)
                .status(this.status)
                .createdAt(this.createdAt);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Booking booking = (Booking) o;
        return Objects.equals(id, booking.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Booking[id=%d, propertyId=%d, userId=%d, dateRange=%s, status=%s, totalPrice=%s]"
                .formatted(id, propertyId, userId, dateRange, status, totalPrice);
    }

    public static final class Builder {
        private Long id;
        private Long propertyId;
        private Long userId;
        private DateRange dateRange;
        private Money totalPrice;
        private BookingStatus status;
        private LocalDateTime createdAt;

        private Builder() {
        }

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder propertyId(Long propertyId) {
            this.propertyId = propertyId;
            return this;
        }

        public Builder userId(Long userId) {
            this.userId = userId;
            return this;
        }

        public Builder dateRange(DateRange dateRange) {
            this.dateRange = dateRange;
            return this;
        }

        public Builder dateRange(LocalDate startDate, LocalDate endDate) {
            this.dateRange = DateRange.of(startDate, endDate);
            return this;
        }

        public Builder totalPrice(Money totalPrice) {
            this.totalPrice = totalPrice;
            return this;
        }

        public Builder status(BookingStatus status) {
            this.status = status;
            return this;
        }

        public Builder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Booking build() {
            return new Booking(this);
        }
    }
}