package com.affiliate.rentals.gydi.properties.application.usecase;

import com.affiliate.rentals.gydi.properties.application.dto.CloudinarySignatureRequest;
import com.affiliate.rentals.gydi.properties.application.dto.CloudinarySignatureResponse;
import com.affiliate.rentals.gydi.properties.domain.model.Property;
import com.affiliate.rentals.gydi.properties.domain.model.PropertyId;
import com.affiliate.rentals.gydi.properties.domain.ports.in.GenerateCloudinarySignaturesUseCase;
import com.affiliate.rentals.gydi.properties.domain.ports.out.PropertyRepositoryPort;
import com.affiliate.rentals.gydi.shared.infrastructure.storage.CloudinaryStorageAdapter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Implementation of {@link GenerateCloudinarySignaturesUseCase}.
 *
 * <p>Generates time-limited signed upload parameters for direct browser-to-Cloudinary uploads.</p>
 */
@Slf4j
@Service
@Transactional(readOnly = true)
public class GenerateCloudinarySignaturesUseCaseImpl implements GenerateCloudinarySignaturesUseCase {

    private final PropertyRepositoryPort propertyRepository;
    private final CloudinaryStorageAdapter cloudinaryAdapter;

    public GenerateCloudinarySignaturesUseCaseImpl(
            PropertyRepositoryPort propertyRepository,
            CloudinaryStorageAdapter cloudinaryAdapter) {
        this.propertyRepository = propertyRepository;
        this.cloudinaryAdapter = cloudinaryAdapter;
    }

    @Override
    public CloudinarySignatureResponse generateSignatures(
            PropertyId propertyId,
            Long userId,
            CloudinarySignatureRequest request) {

        // 1. Verify property exists and user has permission
        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new IllegalArgumentException("Property not found"));

        if (!property.isOwnedBy(userId)) {
            throw new SecurityException("User is not authorized to upload media to this property");
        }

        // 2. Determine folder based on media type
        String baseFolder = "properties/" + propertyId.toString();
        String folder = baseFolder + (request.mediaType() == CloudinarySignatureRequest.MediaType.IMAGE
                ? "/images"
                : "/videos");

        // 3. Generate signatures for each file
        List<CloudinarySignatureResponse.UploadSignature> signatures = new ArrayList<>();

        for (int i = 0; i < request.fileCount(); i++) {
            String publicId = UUID.randomUUID().toString();
            Map<String, Object> signatureData = cloudinaryAdapter.generateUploadSignature(folder, publicId);

            signatures.add(new CloudinarySignatureResponse.UploadSignature(
                    (String) signatureData.get("signature"),
                    (Long) signatureData.get("timestamp"),
                    (String) signatureData.get("folder"),
                    (String) signatureData.get("public_id")
            ));
        }

        // 4. Build response with Cloudinary config
        String cloudName = cloudinaryAdapter.getCloudName();
        String apiKey = cloudinaryAdapter.getApiKey();
        String uploadUrl = String.format("https://api.cloudinary.com/v1_1/%s/auto/upload", cloudName);

        log.info("Generated {} Cloudinary signatures for property {} ({})",
                signatures.size(), propertyId, request.mediaType());

        return new CloudinarySignatureResponse(
                signatures,
                cloudName,
                apiKey,
                uploadUrl
        );
    }
}
