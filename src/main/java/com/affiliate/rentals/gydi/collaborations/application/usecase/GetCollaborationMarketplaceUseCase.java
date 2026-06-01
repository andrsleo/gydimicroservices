package com.affiliate.rentals.gydi.collaborations.application.usecase;

import com.affiliate.rentals.gydi.properties.domain.model.Property;
import com.affiliate.rentals.gydi.properties.domain.ports.out.PropertyRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Use case to list published properties that accept creator collaborations.
 *
 * <p>Powers the public collaboration marketplace page.
 *
 * @author GYDI Development Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GetCollaborationMarketplaceUseCase {

    private final PropertyRepositoryPort propertyRepository;

    @Transactional(readOnly = true)
    public PropertyRepositoryPort.PropertySearchResult execute(String compensationType, int page, int size) {
        return propertyRepository.findAcceptingCollaborations(compensationType, page, size);
    }
}
