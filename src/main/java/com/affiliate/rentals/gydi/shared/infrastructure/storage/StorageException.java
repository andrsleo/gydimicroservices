package com.affiliate.rentals.gydi.shared.infrastructure.storage;

/**
 * Exception thrown when file storage operations fail.
 *
 * @author GYDI Development Team
 */
public class StorageException extends RuntimeException {

    public StorageException(String message) {
        super(message);
    }

    public StorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
