package com.affiliate.rentals.gydi.properties.application.usecase;

import com.affiliate.rentals.gydi.properties.domain.model.Property;
import com.affiliate.rentals.gydi.properties.domain.ports.in.UploadPropertyVideosUseCase;
import com.affiliate.rentals.gydi.properties.domain.ports.out.PropertyRepositoryPort;
import com.affiliate.rentals.gydi.properties.domain.ports.out.PropertyStoragePort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class UploadPropertyVideosUseCaseImpl implements UploadPropertyVideosUseCase {

    private final PropertyRepositoryPort propertyRepository;
    private final PropertyStoragePort storagePort;

    public UploadPropertyVideosUseCaseImpl(PropertyRepositoryPort propertyRepository,
                                          PropertyStoragePort storagePort) {
        this.propertyRepository = propertyRepository;
        this.storagePort = storagePort;
    }

    @Override
    public Property uploadVideos(UploadVideosCommand command) {
        Property property = propertyRepository.findById(command.propertyId())
                .orElseThrow(() -> new IllegalArgumentException("Property not found"));

        if (!property.isOwnedBy(command.requestingUserId())) {
            throw new SecurityException("User is not authorized to modify this property");
        }

        for (VideoUpload video : command.videos()) {
            video.validate();

            String url = storagePort.uploadPropertyVideo(
                command.propertyId().toString(),
                video.fileName(),
                video.contentType(),
                video.data()
            );

            // TODO: Implement actual video thumbnail generation using FFmpeg or external service
            // For now, thumbnails are optional and will be null
            property.addVideo(url, null, video.displayOrder(), null);
        }

        return propertyRepository.save(property);
    }
}
