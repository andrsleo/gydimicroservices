package com.affiliate.rentals.gydi.properties.application.usecase;

import com.affiliate.rentals.gydi.properties.application.dto.ReorderVideosRequest;
import com.affiliate.rentals.gydi.properties.application.dto.ReorderVideosResponse;
import com.affiliate.rentals.gydi.properties.domain.model.Property;
import com.affiliate.rentals.gydi.properties.domain.model.PropertyId;
import com.affiliate.rentals.gydi.properties.domain.ports.out.PropertyRepositoryPort;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
public class ReorderPropertyVideosUseCaseImpl implements ReorderPropertyVideosUseCase {

    private final PropertyRepositoryPort propertyRepository;
    private final EntityManager entityManager;

    public ReorderPropertyVideosUseCaseImpl(PropertyRepositoryPort propertyRepository, EntityManager entityManager) {
        this.propertyRepository = propertyRepository;
        this.entityManager = entityManager;
    }

    @Override
    public ReorderVideosResponse execute(Long propertyId, Long userId, ReorderVideosRequest request) {
        entityManager.createNativeQuery("SET CONSTRAINTS ALL DEFERRED").executeUpdate();

        Property property = propertyRepository.findById(PropertyId.of(propertyId))
                .orElseThrow(() -> new IllegalArgumentException("Property not found: " + propertyId));

        if (!property.isOwnedBy(userId)) {
            throw new SecurityException("User is not authorized to modify this property");
        }

        Map<Long, Integer> orderMap = request.videoOrders().stream()
                .collect(Collectors.toMap(
                    ReorderVideosRequest.VideoOrderItem::videoId,
                    ReorderVideosRequest.VideoOrderItem::displayOrder
                ));

        property.reorderVideos(orderMap);

        propertyRepository.save(property);
        entityManager.flush();

        return ReorderVideosResponse.success(request.videoOrders().size());
    }
}
