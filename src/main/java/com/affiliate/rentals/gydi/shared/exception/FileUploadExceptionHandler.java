package com.affiliate.rentals.gydi.shared.exception;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import lombok.extern.slf4j.Slf4j;

/**
 * Global exception handler for file upload errors.
 *
 * <p>Catches and handles exceptions related to file uploads,
 * providing user-friendly error messages.</p>
 *
 * @author GYDI Development Team
 */
@RestControllerAdvice
@Slf4j
public class FileUploadExceptionHandler {

    /**
     * Handle file size exceeded exception.
     *
     * <p>This exception is thrown by Spring when the uploaded file
     * exceeds the configured maximum size.</p>
     *
     * @param ex the exception
     * @return error response with user-friendly message
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, String>> handleMaxUploadSizeExceeded(
            MaxUploadSizeExceededException ex) {

        log.error("File upload size exceeded: {}", ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(Map.of(
                        "error", "El archivo es demasiado grande",
                        "message", "El tamaño máximo permitido es 10MB. Por favor, selecciona una imagen más pequeña.",
                        "maxSize", "10MB"
                ));
    }
}
