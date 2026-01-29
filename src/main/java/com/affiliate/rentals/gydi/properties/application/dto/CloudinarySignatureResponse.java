package com.affiliate.rentals.gydi.properties.application.dto;

import java.util.List;

/**
 * Response DTO containing Cloudinary upload signatures.
 *
 * <p>The frontend uses these parameters to upload files directly to Cloudinary.
 * Each signature is time-limited (1 hour) and can only be used for a single upload.</p>
 *
 * <p><b>Frontend Upload Flow:</b></p>
 * <ol>
 *   <li>Request signatures from backend</li>
 *   <li>For each file, POST to Cloudinary with signature params + file</li>
 *   <li>Cloudinary returns uploaded file URL</li>
 *   <li>Send URLs back to backend to save in database</li>
 * </ol>
 */
public record CloudinarySignatureResponse(
    List<UploadSignature> signatures,
    String cloudName,
    String apiKey,
    String uploadUrl
) {

    /**
     * Single upload signature with time-limited authentication.
     */
    public record UploadSignature(
        String signature,      // SHA-1 signature for authentication
        long timestamp,        // Unix timestamp (signature valid for 1 hour)
        String folder,         // Cloudinary folder (e.g., "properties/{id}/images")
        String publicId        // Unique public_id for this upload
    ) {}
}
