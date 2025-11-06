package com.affiliate.rentals.gydi.properties.application.usecase;

import com.affiliate.rentals.gydi.properties.application.dto.ReorderImagesRequest;
import com.affiliate.rentals.gydi.properties.application.dto.ReorderImagesResponse;
import com.affiliate.rentals.gydi.properties.domain.model.Property;
import com.affiliate.rentals.gydi.properties.domain.model.PropertyId;
import com.affiliate.rentals.gydi.properties.domain.ports.out.PropertyRepositoryPort;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implementation of ReorderPropertyImagesUseCase.
 * Orchestrates reordering of property images and setting cover image.
 */
@Service
@Transactional
public class ReorderPropertyImagesUseCaseImpl implements ReorderPropertyImagesUseCase {

    private final PropertyRepositoryPort propertyRepository;
    private final EntityManager entityManager;

    public ReorderPropertyImagesUseCaseImpl(PropertyRepositoryPort propertyRepository, EntityManager entityManager) {
        this.propertyRepository = propertyRepository;
        this.entityManager = entityManager;
    }

    @Override
    public ReorderImagesResponse execute(UUID propertyId, Long userId, ReorderImagesRequest request) {
        // 1. Set constraints to deferred for this transaction
        entityManager.createNativeQuery("SET CONSTRAINTS ALL DEFERRED").executeUpdate();

        // 2. Find property
        Property property = propertyRepository.findById(PropertyId.of(propertyId))
                .orElseThrow(() -> new IllegalArgumentException("Property not found: " + propertyId));

        // 3. Verify ownership
        if (!property.isOwnedBy(userId)) {
            throw new SecurityException("User is not authorized to modify this property");
        }

        // 4. Build order map
        Map<UUID, Integer> orderMap = request.imageOrders().stream()
                .collect(Collectors.toMap(
                    ReorderImagesRequest.ImageOrderItem::imageId,
                    ReorderImagesRequest.ImageOrderItem::displayOrder
                ));

        // 5. Reorder images (domain logic validates)
        property.reorderImages(orderMap);

        // 6. Set cover image if provided
        if (request.coverImageId() != null) {
            property.setCoverImage(request.coverImageId());
        }

        // 7. Flush changes to trigger SQL updates
        Property savedProperty = propertyRepository.save(property);
        entityManager.flush();

        // 8. Return response (constraints will be checked at transaction commit)
        return ReorderImagesResponse.success(
            request.imageOrders().size(),
            savedProperty.getCoverImageId()
        );
    }
}