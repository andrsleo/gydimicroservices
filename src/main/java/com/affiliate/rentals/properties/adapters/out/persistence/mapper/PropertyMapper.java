package com.affiliate.rentals.properties.adapters.out.persistence.mapper;

import com.affiliate.rentals.properties.adapters.out.persistence.entity.PropertyEntity;
import com.affiliate.rentals.properties.domain.model.Property;

public class PropertyMapper {

    private PropertyMapper() {
        // utility class: no instantiation
    }

    /**
     * Convierte un PropertyEntity (JPA) en un Property (dominio).
     */
    public static Property toDomain(PropertyEntity entity) {
        if (entity == null) return null;

        return Property.builder()
                .id(entity.getId())
                .ownerId(entity.getOwnerId())
                .title(entity.getTitle())
                .description(entity.getDescription())
                .location(entity.getLocation())
                .pricePerNight(entity.getPricePerNight())
                .currency(entity.getCurrency())
                .bedrooms(entity.getBedrooms())
                .bathrooms(entity.getBathrooms())
                .beds(entity.getBeds())
                .capacity(entity.getCapacity())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    /**
     * Convierte un Property (dominio) en un PropertyEntity (JPA).
     */
    public static PropertyEntity toEntity(Property domain) {
        if (domain == null) return null;

        return PropertyEntity.builder()
                .id(domain.id())
                .ownerId(domain.ownerId())
                .title(domain.title())
                .description(domain.description())
                .location(domain.location())
                .pricePerNight(domain.pricePerNight())
                .currency(domain.currency())
                .bedrooms(domain.bedrooms())
                .bathrooms(domain.bathrooms())
                .beds(domain.beds())
                .capacity(domain.capacity())
                .createdAt(domain.createdAt())
                .build();
    }
}