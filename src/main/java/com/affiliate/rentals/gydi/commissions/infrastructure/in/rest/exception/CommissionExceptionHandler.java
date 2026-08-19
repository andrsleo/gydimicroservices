package com.affiliate.rentals.gydi.commissions.infrastructure.in.rest.exception;

import com.affiliate.rentals.gydi.commissions.domain.exception.CommissionCalculationException;
import com.affiliate.rentals.gydi.commissions.domain.exception.CommissionNotFoundException;
import com.affiliate.rentals.gydi.commissions.domain.exception.InvalidCommissionStateException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;

/**
 * Global exception handler for commission endpoints.
 */
@RestControllerAdvice(basePackages = "com.affiliate.rentals.gydi.commissions.infrastructure.in.rest")
public class CommissionExceptionHandler {

    @ExceptionHandler(CommissionNotFoundException.class)
    public ProblemDetail handleCommissionNotFound(CommissionNotFoundException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
            HttpStatus.NOT_FOUND,
            ex.getMessage()
        );
        problemDetail.setType(URI.create("https://api.gydi.com/errors/commission-not-found"));
        problemDetail.setTitle("Commission Not Found");
        return problemDetail;
    }

    @ExceptionHandler(InvalidCommissionStateException.class)
    public ProblemDetail handleInvalidState(InvalidCommissionStateException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
            HttpStatus.CONFLICT,
            ex.getMessage()
        );
        problemDetail.setType(URI.create("https://api.gydi.com/errors/invalid-commission-state"));
        problemDetail.setTitle("Invalid Commission State");
        return problemDetail;
    }

    @ExceptionHandler(CommissionCalculationException.class)
    public ProblemDetail handleCalculationError(CommissionCalculationException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
            HttpStatus.INTERNAL_SERVER_ERROR,
            ex.getMessage()
        );
        problemDetail.setType(URI.create("https://api.gydi.com/errors/commission-calculation-failed"));
        problemDetail.setTitle("Commission Calculation Failed");
        return problemDetail;
    }
}
