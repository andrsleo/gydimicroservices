package com.affiliate.rentals.gydi.properties.application.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Request DTO for generating Cloudinary upload signatures.
 *
 * <p>This is used to request signed upload parameters that allow
 * the frontend to upload files directly to Cloudinary without
 * going through the backend.</p>
 */
public record CloudinarySignatureRequest(

    @NotNull(message = "Number of files is required")
    @Min(value = 1, message = "Must upload at least 1 file")
    @Max(value = 20, message = "Cannot upload more than 20 files at once")
    Integer fileCount,

    @NotNull(message = "Media type is required")
    MediaType mediaType
) {

    public enum MediaType {
        IMAGE,
        VIDEO
    }
}
