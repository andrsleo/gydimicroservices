package com.affiliate.rentals.gydi.properties.domain.ports.in;

import com.affiliate.rentals.gydi.properties.application.dto.CloudinarySignatureRequest;
import com.affiliate.rentals.gydi.properties.application.dto.CloudinarySignatureResponse;
import com.affiliate.rentals.gydi.properties.domain.model.PropertyId;

/**
 * Use case for generating Cloudinary upload signatures.
 *
 * <p>This enables direct browser-to-Cloudinary uploads, which:
 * <ul>
 *   <li>Reduces backend load (no file processing)</li>
 *   <li>Improves upload speed (parallel uploads, no backend bottleneck)</li>
 *   <li>Enables progress tracking per file</li>
 *   <li>Supports larger files (500MB videos vs 30s timeout)</li>
 * </ul>
 * </p>
 */
public interface GenerateCloudinarySignaturesUseCase {

    /**
     * Generate signed upload parameters for direct Cloudinary uploads.
     *
     * @param propertyId the property ID
     * @param userId the requesting user ID
     * @param request the signature request
     * @return signed upload parameters
     */
    CloudinarySignatureResponse generateSignatures(
        PropertyId propertyId,
        Long userId,
        CloudinarySignatureRequest request
    );
}
