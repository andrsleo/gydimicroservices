package com.affiliate.rentals.gydi.properties.application.usecase;

import com.affiliate.rentals.gydi.properties.domain.model.Property;
import com.affiliate.rentals.gydi.properties.domain.ports.in.UploadPropertyVideosUseCase;
import com.affiliate.rentals.gydi.properties.domain.ports.out.PropertyRepositoryPort;
import com.affiliate.rentals.gydi.properties.domain.ports.out.PropertyStoragePort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class UploadPropertyVideosUseCaseImpl implements UploadPropertyVideosUseCase {

    private static final Logger log = LoggerFactory.getLogger(UploadPropertyVideosUseCaseImpl.class);

    private final PropertyRepositoryPort propertyRepository;
    private final PropertyStoragePort storagePort;

    public UploadPropertyVideosUseCaseImpl(PropertyRepositoryPort propertyRepository,
                                          PropertyStoragePort storagePort) {
        this.propertyRepository = propertyRepository;
        this.storagePort = storagePort;
    }

    @Override
    public Property uploadVideos(UploadVideosCommand command) {
        log.info("uploadVideos - propertyId: {}, requestingUserId: {}, videoCount: {}",
                command.propertyId(), command.requestingUserId(), command.videos().size());

        Property property = propertyRepository.findById(command.propertyId())
                .orElseThrow(() -> new IllegalArgumentException("Property not found"));

        log.info("uploadVideos - Property found. hostId: {}, requestingUserId: {}, isOwner: {}",
                property.getHostId(), command.requestingUserId(), property.isOwnedBy(command.requestingUserId()));

        if (!property.isOwnedBy(command.requestingUserId())) {
            log.warn("uploadVideos - OWNERSHIP CHECK FAILED! hostId={} != requestingUserId={}",
                    property.getHostId(), command.requestingUserId());
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
