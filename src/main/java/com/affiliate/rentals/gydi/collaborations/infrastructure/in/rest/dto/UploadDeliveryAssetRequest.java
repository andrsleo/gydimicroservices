package com.affiliate.rentals.gydi.collaborations.infrastructure.in.rest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

public record UploadDeliveryAssetRequest(
        @NotBlank @Pattern(regexp = "^https://.*", message = "fileUrl must use HTTPS") String fileUrl,
        @NotBlank String fileType,
        @NotNull @Positive Long fileSizeBytes
) {}
