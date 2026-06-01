package com.affiliate.rentals.gydi.collaborations.infrastructure.in.rest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record UploadDeliveryAssetRequest(
        @NotBlank String fileUrl,
        @NotBlank String fileType,
        @NotNull @Positive Long fileSizeBytes
) {}
