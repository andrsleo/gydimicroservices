package com.affiliate.rentals.gydi.bookings.infrastructure.out.persistence.mapper;

import com.affiliate.rentals.gydi.bookings.domain.model.Booking;
import com.affiliate.rentals.gydi.bookings.domain.model.BookingStatusHistory;
import com.affiliate.rentals.gydi.bookings.domain.model.vo.BookingDates;
import com.affiliate.rentals.gydi.bookings.domain.model.vo.GuestInfo;
import com.affiliate.rentals.gydi.bookings.domain.model.vo.Money;
import com.affiliate.rentals.gydi.bookings.infrastructure.out.persistence.entity.BookingJpaEntity;
import com.affiliate.rentals.gydi.bookings.infrastructure.out.persistence.entity.BookingStatusHistoryJpaEntity;
import org.mapstruct.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * MapStruct mapper for converting between JPA entities and domain models.
 * <p>
 * Critical for maintaining separation between infrastructure and domain layers.
 * </p>
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface BookingEntityMapper {

    /**
     * Converts JPA entity to domain model using reconstitution.
     */
    default Booking toDomain(BookingJpaEntity entity) {
        if (entity == null) {
            return null;
        }

        Booking booking = Booking.reconstitute(
                entity.getId(),
                entity.getReferralLinkId(),
                entity.getPropertyId(),
                mapBookingDates(entity),
                mapGuestInfo(entity),
                entity.getAirbnbConfirmationCode(),
                mapMoney(entity.getTotalAmount(), entity.getCurrency()),
                entity.getHostCommissionRate(),
                entity.getAffiliateCommissionRate(),
                entity.getStatus(),
                toHistoryDomainList(entity.getStatusHistory()),
                entity.getReservedAt(),
                entity.getStartedAt(),
                entity.getFinishedAt(),
                entity.getCancelledAt(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());

        // Restore Fase 1 fields
        booking.setRejectionReason(entity.getRejectionReason());

        // Restore Phase 4 — Social Commerce
        booking.setContentPostId(entity.getContentPostId());

        // Restore Fase 2 Stripe fields
        booking.setStripeBookingIntentId(entity.getStripeBookingIntentId());
        booking.setStripeDepositIntentId(entity.getStripeDepositIntentId());
        if (entity.getDepositAmount() != null) {
            String currency = entity.getDepositCurrency() != null ? entity.getDepositCurrency() : "USD";
            booking.setDepositAmount(mapMoney(entity.getDepositAmount(), currency));
        }
        booking.setDepositCapturedAt(entity.getDepositCapturedAt());
        booking.setDepositCaptureAmount(entity.getDepositCaptureAmount());
        booking.setPaymentReleasedAt(entity.getPaymentReleasedAt());

        return booking;
    }

    /**
     * Converts domain model to JPA entity.
     */
    @Mapping(target = "checkInDate", source = "bookingDates.checkInDate")
    @Mapping(target = "checkOutDate", source = "bookingDates.checkOutDate")
    @Mapping(target = "guestName", source = "guestInfo.name")
    @Mapping(target = "guestEmail", source = "guestInfo.email")
    @Mapping(target = "guestPhone", source = "guestInfo.phone")
    @Mapping(target = "guestsCount", source = "guestInfo.guestsCount")
    @Mapping(target = "totalAmount", expression = "java(domain.getTotalAmount() != null ? domain.getTotalAmount().getAmount() : null)")
    @Mapping(target = "currency", expression = "java(domain.getTotalAmount() != null ? domain.getTotalAmount().getCurrency() : \"USD\")")
    @Mapping(target = "statusHistory", ignore = true) // Handled separately
    // Fase 1 — Rejection
    @Mapping(target = "rejectionReason", source = "rejectionReason")
    // Fase 2 — Stripe
    @Mapping(target = "stripeBookingIntentId", source = "stripeBookingIntentId")
    @Mapping(target = "stripeDepositIntentId", source = "stripeDepositIntentId")
    @Mapping(target = "depositAmount", expression = "java(domain.getDepositAmount() != null ? domain.getDepositAmount().getAmount() : null)")
    @Mapping(target = "depositCurrency", expression = "java(domain.getDepositAmount() != null ? domain.getDepositAmount().getCurrency() : null)")
    @Mapping(target = "depositCapturedAt", source = "depositCapturedAt")
    @Mapping(target = "depositCaptureAmount", source = "depositCaptureAmount")
    @Mapping(target = "paymentReleasedAt", source = "paymentReleasedAt")
    // Phase 4 — Social Commerce
    @Mapping(target = "contentPostId", source = "contentPostId")
    BookingJpaEntity toEntity(Booking domain);

    /**
     * Converts JPA history entity to domain model using reconstitution.
     */
    default BookingStatusHistory toDomain(BookingStatusHistoryJpaEntity entity) {
        if (entity == null) {
            return null;
        }

        return BookingStatusHistory.reconstitute(
                entity.getId(),
                entity.getBooking() != null ? entity.getBooking().getId() : null,
                entity.getOldStatus(),
                entity.getNewStatus(),
                entity.getChangedBy(),
                entity.getChangeReason(),
                entity.getIsAutomatic() != null ? entity.getIsAutomatic() : false,
                entity.getCreatedAt());
    }

    /**
     * Converts domain history to JPA entity.
     */
    @Mapping(target = "booking", ignore = true) // Set manually in adapter
    @Mapping(target = "isAutomatic", source = "automatic")
    BookingStatusHistoryJpaEntity toEntity(BookingStatusHistory domain);

    /**
     * Maps list of JPA entities to domain models.
     */
    List<Booking> toDomainList(List<BookingJpaEntity> entities);

    /**
     * Maps list of JPA history entities to domain models.
     */
    List<BookingStatusHistory> toHistoryDomainList(List<BookingStatusHistoryJpaEntity> entities);

    // Custom mapping methods

    default BookingDates mapBookingDates(BookingJpaEntity entity) {
        if (entity.getCheckInDate() == null || entity.getCheckOutDate() == null) {
            return null;
        }
        // Use reconstitute() to load from DB without future date validation
        return BookingDates.reconstitute(entity.getCheckInDate(), entity.getCheckOutDate());
    }

    default GuestInfo mapGuestInfo(BookingJpaEntity entity) {
        if (entity.getGuestName() == null || entity.getGuestEmail() == null) {
            return null;
        }
        return GuestInfo.of(
                entity.getGuestName(),
                entity.getGuestEmail(),
                entity.getGuestPhone(),
                entity.getGuestsCount() != null ? entity.getGuestsCount() : 1);
    }

    default Money mapMoney(BigDecimal amount, String currency) {
        if (amount == null) {
            return null;
        }
        return Money.of(amount, currency != null ? currency : "USD");
    }
}
