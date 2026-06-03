package com.affiliate.rentals.gydi.bookings.infrastructure.in.rest.exception;

import com.affiliate.rentals.gydi.bookings.domain.exception.BookingAccessDeniedException;
import com.affiliate.rentals.gydi.bookings.domain.exception.BookingNotFoundException;
import com.affiliate.rentals.gydi.bookings.domain.exception.DateRangeNotAvailableException;
import com.affiliate.rentals.gydi.bookings.domain.exception.InvalidBookingStateException;
import com.affiliate.rentals.gydi.bookings.domain.exception.InvalidBookingStatusTransitionException;
import com.affiliate.rentals.gydi.bookings.domain.exception.InvalidCalendarOperationException;
import com.affiliate.rentals.gydi.bookings.domain.exception.UnauthorizedPropertyAccessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

/**
 * Global exception handler for bookings bounded context.
 * <p>
 * Converts domain exceptions to HTTP responses using RFC 9457 Problem Details.
 * </p>
 */
@RestControllerAdvice(basePackages = "com.affiliate.rentals.gydi.bookings.infrastructure.in.rest")
public class BookingExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(BookingExceptionHandler.class);

    @ExceptionHandler(BookingNotFoundException.class)
    public ProblemDetail handleBookingNotFound(BookingNotFoundException ex) {
        log.warn("Booking not found: {}", ex.getMessage());

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.NOT_FOUND,
            ex.getMessage()
        );
        problem.setTitle("Booking Not Found");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(InvalidBookingStatusTransitionException.class)
    public ProblemDetail handleInvalidStatusTransition(InvalidBookingStatusTransitionException ex) {
        log.warn("Invalid booking status transition: {}", ex.getMessage());

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.BAD_REQUEST,
            ex.getMessage()
        );
        problem.setTitle("Invalid Booking Status Transition");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("Invalid argument: {}", ex.getMessage());

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.BAD_REQUEST,
            ex.getMessage()
        );
        problem.setTitle("Invalid Request");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(BookingAccessDeniedException.class)
    public ProblemDetail handleBookingAccessDenied(BookingAccessDeniedException ex) {
        log.warn("Booking access denied: {}", ex.getMessage());

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.FORBIDDEN,
            "Access denied to this booking"
        );
        problem.setTitle("Booking Access Denied");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(UnauthorizedPropertyAccessException.class)
    public ProblemDetail handleUnauthorizedPropertyAccess(UnauthorizedPropertyAccessException ex) {
        log.warn("Unauthorized property access: {}", ex.getMessage());

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.FORBIDDEN,
            "You do not have permission to manage this property"
        );
        problem.setTitle("Unauthorized Property Access");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(InvalidCalendarOperationException.class)
    public ProblemDetail handleInvalidCalendarOperation(InvalidCalendarOperationException ex) {
        log.warn("Invalid calendar operation: {}", ex.getMessage());

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.CONFLICT,
            ex.getMessage()
        );
        problem.setTitle("Invalid Calendar Operation");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(DateRangeNotAvailableException.class)
    public ProblemDetail handleDateRangeNotAvailable(DateRangeNotAvailableException ex) {
        log.warn("Date range not available: {}", ex.getMessage());

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.CONFLICT,
            ex.getMessage()
        );
        problem.setTitle("Dates Not Available");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(InvalidBookingStateException.class)
    public ProblemDetail handleInvalidBookingState(InvalidBookingStateException ex) {
        log.warn("Invalid booking state for operation: {}", ex.getMessage());

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.CONFLICT,
            ex.getMessage()
        );
        problem.setTitle("Invalid Booking State");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }
}
