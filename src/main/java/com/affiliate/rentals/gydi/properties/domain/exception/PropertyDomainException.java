package com.affiliate.rentals.gydi.properties.domain.exception;

/**
 * Base sealed exception for all domain-level exceptions in the properties bounded context.
 *
 * <p>Follows the sealed class pattern from users bounded context for exhaustive handling
 * and type safety with Java 21.</p>
 *
 * @author GYDI Development Team
 */
public sealed abstract class PropertyDomainException extends RuntimeException
        permits PropertyCannotBePublishedException,
                PropertyNotFoundException,
                PropertyNotOwnedException {

    protected PropertyDomainException(String message) {
        super(message);
    }

    protected PropertyDomainException(String message, Throwable cause) {
        super(message, cause);
    }
}