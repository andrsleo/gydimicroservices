package com.affiliate.rentals.gydi.properties.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;

import java.util.List;

/**
 * Command to save media URLs after frontend has uploaded to Cloudinary.
 *
 * <p>This is called after the frontend successfully uploads files directly
 * to Cloudinary and receives the URLs. The backend then stores these URLs
 * in the database.</p>
 */
public record SaveUploadedMediaCommand(

    @NotEmpty(message = "At least one media URL is required")
    List<MediaUrl> mediaUrls
) {

    public record MediaUrl(

        @NotBlank(message = "URL is required")
        @Pattern(regexp = "^https://res\\.cloudinary\\.com/.*",
                message = "Must be a valid Cloudinary URL")
        String url,

        int displayOrder,

        Integer durationSeconds
    ) {}
}
