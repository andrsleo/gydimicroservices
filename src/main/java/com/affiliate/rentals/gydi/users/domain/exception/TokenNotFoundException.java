package com.affiliate.rentals.gydi.users.domain.exception;

public class TokenNotFoundException extends AuthenticationException {
    public TokenNotFoundException() {
        super("No authentication token found in request");
    }
}
