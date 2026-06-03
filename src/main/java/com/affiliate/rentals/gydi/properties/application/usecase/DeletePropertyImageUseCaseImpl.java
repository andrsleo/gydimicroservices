package com.affiliate.rentals.gydi.properties.application.usecase;

import com.affiliate.rentals.gydi.properties.domain.model.Property;
import com.affiliate.rentals.gydi.properties.domain.model.PropertyId;
import com.affiliate.rentals.gydi.properties.domain.model.PropertyImage;
import com.affiliate.rentals.gydi.properties.domain.ports.out.PropertyRepositoryPort;
import com.affiliate.rentals.gydi.shared.domain.port.StoragePort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of DeletePropertyImageUseCase.
 * Deletes image from both database and storage (S3/local filesystem).
 *
 * After deletion, triggers a status demotion check: if the property was in
 * PENDING_APPROVAL and now has fewer than the minimum required images, it is
 * reverted to DRAFT so the host can upload more images before re-submission.
 */
@Service
@Transactional
public class DeletePropertyImageUseCaseImpl implements DeletePropertyImageUseCase {

    private final PropertyRepositoryPort propertyRepository;
    private final StoragePort storagePort;

    public DeletePropertyImageUseCaseImpl(PropertyRepositoryPort propertyRepository, StoragePort storagePort) {
        this.propertyRepository = propertyRepository;
        this.storagePort = storagePort;
    }

    @Override
    public void execute(Long propertyId, Long imageId, Long userId) {
        // 1. Find property
        Property property = propertyRepository.findById(PropertyId.of(propertyId))
                .orElseThrow(() -> new IllegalArgumentException("Property not found: " + propertyId));

        // 2. Verify ownership
        if (!property.isOwnedBy(userId)) {
            throw new SecurityException("User is not authorized to modify this property");
        }

        // 3. Find image to delete
        PropertyImage imageToDelete = property.getImages().stream()
                .filter(img -> img.getId().equals(imageId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Image not found: " + imageId));

        // 4. Delete file from storage (S3 or local)
        try {
            storagePort.deleteFileByUrl(imageToDelete.getUrl());
        } catch (Exception e) {
            System.err.println("Failed to delete file from storage: " + e.getMessage());
        }

        // 5. Remove image from property (domain logic)
        property.removeImage(imageId);

        // 6. If this was the cover image, clear coverImageId
        if (property.getCoverImageId() != null && property.getCoverImageId().equals(imageId)) {
            property.setCoverImage(null);
        }

        // 7. Demote PENDING_APPROVAL -> DRAFT if image count dropped below minimum
        property.demoteToDraftIfMinimumsBroken();

        // 8. Save updated property
        propertyRepository.save(property);
    }
}
