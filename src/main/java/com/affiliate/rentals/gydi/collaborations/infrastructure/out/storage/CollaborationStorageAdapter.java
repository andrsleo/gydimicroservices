package com.affiliate.rentals.gydi.collaborations.infrastructure.out.storage;

import com.affiliate.rentals.gydi.collaborations.application.port.out.CollaborationStoragePort;
import com.affiliate.rentals.gydi.shared.domain.port.StoragePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

/**
 * Adapter implementing {@link CollaborationStoragePort} using the shared {@link StoragePort}.
 *
 * <p>Uploads delivery assets to the folder {@code collaborations/{agreementId}/}
 * in the configured storage backend (Cloudinary in production).
 *
 * @author GYDI Development Team
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CollaborationStorageAdapter implements CollaborationStoragePort {

    private final StoragePort storagePort;

    @Override
    public String uploadDeliveryAsset(MultipartFile file, Long agreementId) {
        String folder = "collaborations/" + agreementId;
        log.debug("Uploading delivery asset to folder '{}': filename={}, size={}",
                folder, file.getOriginalFilename(), file.getSize());
        String url = storagePort.uploadFile(file, folder);
        log.info("Delivery asset uploaded: agreementId={}, url={}", agreementId, url);
        return url;
    }
}
