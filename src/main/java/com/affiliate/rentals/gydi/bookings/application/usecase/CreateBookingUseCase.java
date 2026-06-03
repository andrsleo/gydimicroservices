package com.affiliate.rentals.gydi.bookings.application.usecase;

import com.affiliate.rentals.gydi.bookings.application.dto.BookingDto;
import com.affiliate.rentals.gydi.bookings.application.dto.CreateBookingRequest;
import com.affiliate.rentals.gydi.bookings.application.mapper.BookingMapper;
import com.affiliate.rentals.gydi.bookings.domain.exception.DateRangeNotAvailableException;
import com.affiliate.rentals.gydi.bookings.domain.exception.PropertyNotAvailableException;
import com.affiliate.rentals.gydi.bookings.domain.model.Booking;
import com.affiliate.rentals.gydi.bookings.domain.model.vo.BookingDates;
import com.affiliate.rentals.gydi.bookings.domain.model.vo.GuestInfo;
import com.affiliate.rentals.gydi.bookings.domain.ports.BookingRepositoryPort;
import com.affiliate.rentals.gydi.bookings.domain.ports.PropertyCalendarRepositoryPort;
import com.affiliate.rentals.gydi.content.application.port.ContentPostRepositoryPort;
import com.affiliate.rentals.gydi.content.domain.model.ContentPost;
import com.affiliate.rentals.gydi.properties.domain.exception.PropertyNotFoundException;
import com.affiliate.rentals.gydi.properties.domain.model.Property;
import com.affiliate.rentals.gydi.properties.domain.model.PropertyId;
import com.affiliate.rentals.gydi.properties.domain.model.PropertyStatus;
import com.affiliate.rentals.gydi.properties.domain.ports.out.PropertyRepositoryPort;
import com.affiliate.rentals.gydi.shared.events.BookingFromContentEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

/**
 * Use Case: Create a new booking request.
 * <p>
 * Business Flow:
 * 1. Validate input (referral link and property exist)
 * 2. Create Booking aggregate in REQUEST status
 * 3. Persist booking
 * 4. Return booking DTO
 * </p>
 */
@Service
public class CreateBookingUseCase {
        private static final Logger log = LoggerFactory.getLogger(CreateBookingUseCase.class);

        private final BookingRepositoryPort bookingRepository;
        private final BookingMapper bookingMapper;
        private final PropertyRepositoryPort propertyRepository;
        private final PropertyCalendarRepositoryPort calendarRepository;
        private final ContentPostRepositoryPort contentPostRepository;
        private final ApplicationEventPublisher eventPublisher;

        public CreateBookingUseCase(
                        BookingRepositoryPort bookingRepository,
                        BookingMapper bookingMapper,
                        PropertyRepositoryPort propertyRepository,
                        PropertyCalendarRepositoryPort calendarRepository,
                        ContentPostRepositoryPort contentPostRepository,
                        ApplicationEventPublisher eventPublisher) {
                this.bookingRepository = Objects.requireNonNull(bookingRepository);
                this.bookingMapper = Objects.requireNonNull(bookingMapper);
                this.propertyRepository = Objects.requireNonNull(propertyRepository);
                this.calendarRepository = Objects.requireNonNull(calendarRepository);
                this.contentPostRepository = Objects.requireNonNull(contentPostRepository);
                this.eventPublisher = Objects.requireNonNull(eventPublisher);
        }

        /**
         * Executes the use case.
         *
         * @param request create booking command
         * @return created booking DTO
         * @throws IllegalArgumentException if validation fails
         */
        @Transactional
        public BookingDto execute(CreateBookingRequest request) {
                log.info("Creating booking request for property {} via referral link {}",
                                request.propertyId(), request.referralLinkId());

                // Create value objects (validates dates: future check-in, max 30 days, etc.)
                BookingDates bookingDates = BookingDates.of(request.checkInDate(), request.checkOutDate());
                GuestInfo guestInfo = GuestInfo.of(
                                request.guestName(),
                                request.guestEmail(),
                                request.guestPhone(),
                                request.guestsCount());

                // ✅ SECURITY FIX: Validate property exists and is PUBLISHED
                validatePropertyAvailability(request.propertyId());

                // ✅ SECURITY FIX: Check for overlapping bookings
                validateNoOverlappingBookings(request.propertyId(), bookingDates);

                // ✅ Validate PropertyCalendar: reject if any date in range is manually blocked
                validateCalendarAvailability(request.propertyId(), bookingDates);

                // Create domain model
                Booking booking = Booking.createRequest(
                                request.referralLinkId(),
                                request.propertyId(),
                                bookingDates,
                                guestInfo);

                // Phase 4 — Social Commerce: link content post if provided
                Long creatorUserId = null;
                if (request.contentPostId() != null) {
                        ContentPost post = contentPostRepository.findById(request.contentPostId())
                                .orElse(null);
                        if (post != null) {
                                booking.setContentPostId(request.contentPostId());
                                creatorUserId = post.creatorId();
                        } else {
                                log.warn("Content post {} not found — booking created without content attribution",
                                        request.contentPostId());
                        }
                }

                // Persist booking (first save assigns the database ID)
                Booking saved = bookingRepository.save(booking);

                // Add initial status history now that we have a DB-assigned ID
                saved.addInitialStatusHistory();

                // Save again to persist the status history entry
                saved = bookingRepository.save(saved);

                log.info("Booking created successfully with ID: {}", saved.getId());

                // Phase 4 — publish BookingFromContentEvent if content-attributed
                if (saved.getContentPostId() != null && creatorUserId != null) {
                        eventPublisher.publishEvent(new BookingFromContentEvent(
                                saved.getId(),
                                saved.getContentPostId(),
                                creatorUserId,
                                saved.getPropertyId()
                        ));
                }

                // Map to DTO
                return bookingMapper.toDto(saved);
        }

        /**
         * Validates that the property exists and is published (available for booking).
         *
         * ✅ SECURITY FIX: Prevents bookings for non-existent or unpublished properties.
         *
         * @param propertyId the property ID
         * @throws PropertyNotFoundException     if property doesn't exist
         * @throws PropertyNotAvailableException if property is not published
         */
        private void validatePropertyAvailability(Long propertyId) {
                Property property = propertyRepository.findById(PropertyId.of(propertyId))
                                .orElseThrow(() -> new PropertyNotFoundException(String.valueOf(propertyId)));

                if (property.getStatus() != PropertyStatus.PUBLISHED) {
                        throw new PropertyNotAvailableException(
                                        String.format("Property %d is not available for booking. Current status: %s",
                                                        propertyId, property.getStatus()));
                }
        }

        /**
         * Validates that the requested dates are not blocked in the property calendar.
         *
         * @param propertyId   the property ID
         * @param bookingDates the requested booking dates
         * @throws DateRangeNotAvailableException if any date in the range is blocked
         */
        private void validateCalendarAvailability(Long propertyId, BookingDates bookingDates) {
                boolean hasConflict = calendarRepository.hasAnyBlockedDate(
                                propertyId,
                                bookingDates.getCheckInDate(),
                                bookingDates.getCheckOutDate());

                if (hasConflict) {
                        throw new DateRangeNotAvailableException(
                                        "Property " + propertyId +
                                        " has blocked dates between " + bookingDates.getCheckInDate() +
                                        " and " + bookingDates.getCheckOutDate());
                }
        }

        /**
         * Validates that there are no overlapping bookings for the same property and
         * dates.
         *
         * ✅ SECURITY FIX: Prevents double-booking attacks.
         *
         * @param propertyId   the property ID
         * @param bookingDates the requested booking dates
         * @throws PropertyNotAvailableException if property has overlapping bookings
         */
        private void validateNoOverlappingBookings(Long propertyId, BookingDates bookingDates) {
                boolean hasOverlap = bookingRepository.existsOverlappingBooking(
                                propertyId,
                                bookingDates.getCheckInDate(),
                                bookingDates.getCheckOutDate());

                if (hasOverlap) {
                        throw new PropertyNotAvailableException(
                                        String.format("Property %d is not available for the requested dates (%s to %s). "
                                                        +
                                                        "There is a conflicting booking.",
                                                        propertyId,
                                                        bookingDates.getCheckInDate(),
                                                        bookingDates.getCheckOutDate()));
                }
        }
}
