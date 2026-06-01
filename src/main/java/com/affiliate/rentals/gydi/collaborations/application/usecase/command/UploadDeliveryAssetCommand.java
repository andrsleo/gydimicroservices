package com.affiliate.rentals.gydi.collaborations.application.usecase.command;

/**
 * Command to upload a delivery asset for a specific agreement deliverable.
 * File has already been uploaded to storage; this command carries the resulting URL.
 */
public record UploadDeliveryAssetCommand(
        Long agreementId,
        Long deliverableId,
        Long requestingUserId,
        String fileUrl,
        String fileType,
        long fileSizeBytes
) {}
