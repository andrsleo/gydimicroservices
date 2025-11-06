package com.affiliate.rentals.gydi.properties.application.usecase;

import com.affiliate.rentals.gydi.properties.domain.model.Property;
import com.affiliate.rentals.gydi.properties.domain.model.PropertyId;
import com.affiliate.rentals.gydi.properties.domain.model.PropertyVideo;
import com.affiliate.rentals.gydi.properties.domain.ports.out.PropertyRepositoryPort;
import com.affiliate.rentals.gydi.shared.domain.port.StoragePort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Implementation of DeletePropertyVideoUseCase.
 * Deletes video from both database and storage (S3/local filesystem).
 */
@Service
@Transactional
public class DeletePropertyVideoUseCaseImpl implements DeletePropertyVideoUseCase {

    private final PropertyRepositoryPort propertyRepository;
    private final StoragePort storagePort;

    public DeletePropertyVideoUseCaseImpl(PropertyRepositoryPort propertyRepository, StoragePort storagePort) {
        this.propertyRepository = propertyRepository;
        this.storagePort = storagePort;
    }

    @Override
    public void execute(UUID propertyId, UUID videoId, Long userId) {
        // 1. Find property
        Property property = propertyRepository.findById(PropertyId.of(propertyId))
                .orElseThrow(() -> new IllegalArgumentException("Property not found: " + propertyId));

        // 2. Verify ownership
        if (!property.isOwnedBy(userId)) {
            throw new SecurityException("User is not authorized to modify this property");
        }

        // 3. Find video to delete
        PropertyVideo videoToDelete = property.getVideos().stream()
                .filter(vid -> vid.getId().equals(videoId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Video not found: " + videoId));

        // 4. Delete video file from storage
        try {
            storagePort.deleteFileByUrl(videoToDelete.getUrl());
        } catch (Exception e) {
            System.err.println("Failed to delete video from storage: " + e.getMessage());
        }

        // 5. Delete thumbnail if exists
        if (videoToDelete.getThumbnailUrl() != null) {
            try {
                storagePort.deleteFileByUrl(videoToDelete.getThumbnailUrl());
            } catch (Exception e) {
                System.err.println("Failed to delete thumbnail from storage: " + e.getMessage());
            }
        }

        // 6. Remove video from property
        property.removeVideo(videoId);

        // 7. Save updated property
        propertyRepository.save(property);
    }
}
