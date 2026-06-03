package com.affiliate.rentals.gydi.bookings.application.mapper;

import com.affiliate.rentals.gydi.bookings.application.dto.BookingDto;
import com.affiliate.rentals.gydi.bookings.application.dto.BookingStatusHistoryDto;
import com.affiliate.rentals.gydi.bookings.domain.model.Booking;
import com.affiliate.rentals.gydi.bookings.domain.model.BookingStatusHistory;
import org.mapstruct.*;

import java.util.List;

/**
 * MapStruct mapper for Booking aggregate.
 * <p>
 * Maps between domain models and DTOs.
 * </p>
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface BookingMapper {

    /**
     * Maps Booking domain model to BookingDto.
     *
     * @param booking domain model
     * @return booking DTO
     */
    @Mapping(target = "checkInDate", source = "bookingDates.checkInDate")
    @Mapping(target = "checkOutDate", source = "bookingDates.checkOutDate")
    @Mapping(target = "numberOfNights", expression = "java((int)booking.getBookingDates().getNumberOfNights())")
    @Mapping(target = "guestName", source = "guestInfo.name")
    @Mapping(target = "guestEmail", source = "guestInfo.email")
    @Mapping(target = "guestPhone", source = "guestInfo.phone")
    @Mapping(target = "guestsCount", source = "guestInfo.guestsCount")
    @Mapping(target = "totalAmount", source = "totalAmount.amount")
    @Mapping(target = "currency", source = "totalAmount.currency")
    @Mapping(target = "statusHistory", source = "statusHistory")
    // Fase 1
    @Mapping(target = "rejectionReason", source = "rejectionReason")
    // Fase 2 — Stripe
    @Mapping(target = "stripeBookingIntentId", source = "stripeBookingIntentId")
    @Mapping(target = "stripeDepositIntentId", source = "stripeDepositIntentId")
    @Mapping(target = "depositAmount", source = "depositAmount.amount")
    @Mapping(target = "depositCurrency", source = "depositAmount.currency")
    @Mapping(target = "depositCapturedAt", source = "depositCapturedAt")
    @Mapping(target = "depositCaptureAmount", source = "depositCaptureAmount")
    @Mapping(target = "paymentReleasedAt", source = "paymentReleasedAt")
    BookingDto toDto(Booking booking);

    /**
     * Maps list of Booking domain models to BookingDto list.
     */
    List<BookingDto> toDtoList(List<Booking> bookings);

    /**
     * Maps BookingStatusHistory domain model to DTO.
     */
    @Mapping(target = "isAutomatic", source = "automatic")
    BookingStatusHistoryDto toDto(BookingStatusHistory history);

    /**
     * Maps list of BookingStatusHistory to DTO list.
     */
    List<BookingStatusHistoryDto> toHistoryDtoList(List<BookingStatusHistory> historyList);
}
