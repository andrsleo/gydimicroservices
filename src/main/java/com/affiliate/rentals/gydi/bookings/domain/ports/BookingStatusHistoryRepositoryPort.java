package com.affiliate.rentals.gydi.bookings.domain.ports;

import com.affiliate.rentals.gydi.bookings.domain.model.BookingStatusHistory;

import java.util.List;

/**
 * Repository Port for BookingStatusHistory.
 * <p>
 * Defines persistence operations without infrastructure dependencies.
 * Implemented by infrastructure layer (BookingStatusHistoryRepositoryAdapter).
 * </p>
 */
public interface BookingStatusHistoryRepositoryPort {

    /**
     * Saves a status history entry.
     *
     * @param history the status history to save
     * @return the saved status history with assigned ID
     */
    BookingStatusHistory save(BookingStatusHistory history);

    /**
     * Finds all status history entries for a booking.
     * <p>
     * Returns entries ordered by creation date (oldest first).
     * </p>
     *
     * @param bookingId the booking ID
     * @return list of status history entries
     */
    List<BookingStatusHistory> findByBookingId(Long bookingId);

    /**
     * Finds status history entries changed by a specific user.
     *
     * @param userId the user ID
     * @return list of status history entries
     */
    List<BookingStatusHistory> findByChangedBy(Long userId);
}
