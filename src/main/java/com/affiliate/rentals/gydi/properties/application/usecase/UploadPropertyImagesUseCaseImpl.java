package com.affiliate.rentals.gydi.properties.application.usecase;

import com.affiliate.rentals.gydi.properties.domain.model.Property;
import com.affiliate.rentals.gydi.properties.domain.ports.in.UploadPropertyImagesUseCase;
import com.affiliate.rentals.gydi.properties.domain.ports.out.PropertyRepositoryPort;
import com.affiliate.rentals.gydi.properties.domain.ports.out.PropertyStoragePort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class UploadPropertyImagesUseCaseImpl implements UploadPropertyImagesUseCase {

    private final PropertyRepositoryPort propertyRepository;
    private final PropertyStoragePort storagePort;

    public UploadPropertyImagesUseCaseImpl(PropertyRepositoryPort propertyRepository, 
                                          PropertyStoragePort storagePort) {
        this.propertyRepository = propertyRepository;
        this.storagePort = storagePort;
    }

    @Override
    public Property uploadImages(UploadImagesCommand command) {
        Property property = propertyRepository.findById(command.propertyId())
                .orElseThrow(() -> new IllegalArgumentException("Property not found"));

        if (!property.isOwnedBy(command.requestingUserId())) {
            throw new SecurityException("User is not authorized to modify this property");
        }

        for (ImageUpload image : command.images()) {
            image.validate();
            
            String url = storagePort.uploadPropertyImage(
                command.propertyId().toString(),
                image.fileName(),
                image.contentType(),
                image.data()
            );
            
            property.addImage(url, image.displayOrder());
        }

        return propertyRepository.save(property);
    }
}
