package com.affiliate.rentals.gydi.calendar.domain.exception;

import com.affiliate.rentals.gydi.shared.exception.HttpStatusMapping;
import org.springframework.http.HttpStatus;

@HttpStatusMapping(status = HttpStatus.NOT_FOUND, errorType = "Season Definition Not Found")
public class SeasonDefinitionNotFoundException extends RuntimeException {
    public SeasonDefinitionNotFoundException(Long id) {
        super("Season definition not found: " + id);
    }
}
