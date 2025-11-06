package com.affiliate.rentals.gydi.properties.infrastructure.out.persistence;

import com.affiliate.rentals.gydi.properties.domain.model.*;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class PropertyJpaMapper {

    private final AmenityJpaRepository amenityRepository;

    public PropertyJpaMapper(AmenityJpaRepository amenityRepository) {
        this.amenityRepository = amenityRepository;
    }

    public Property toDomain(PropertyJpaEntity entity) {
        List<PropertyImage> images = entity.getImages().stream()
            .map(this::toImageDomain)
            .collect(Collectors.toList());

        List<PropertyVideo> videos = entity.getVideos().stream()
            .map(this::toVideoDomain)
            .collect(Collectors.toList());

        // Convert Set<AmenityJpaEntity> to List<String>
        List<String> amenityNames = entity.getAmenities().stream()
            .map(AmenityJpaEntity::getName)
            .collect(Collectors.toList());

        return Property.builder()
            .id(PropertyId.of(entity.getId()))
            .hostId(entity.getHostId())
            .title(entity.getTitle())
            .description(entity.getDescription())
            .pricePerNight(Money.of(entity.getPriceAmount(), entity.getPriceCurrency()))
            .location(PropertyLocation.of(entity.getCountry(), entity.getCity(), entity.getAddress(), entity.getPostalCode()))
            .amenities(amenityNames)
            .specs(PropertySpecs.of(entity.getBedrooms(), entity.getBathrooms(), entity.getMaxGuests()))
            .propertyType(PropertyType.valueOf(entity.getPropertyType()))
            .status(PropertyStatus.valueOf(entity.getStatus()))
            .images(images)
            .videos(videos)
            .coverImageId(entity.getCoverImageId())
            .createdAt(entity.getCreatedAt())
            .updatedAt(entity.getUpdatedAt())
            .publishedAt(entity.getPublishedAt())
            .build();
    }

    public PropertyJpaEntity toEntity(Property domain) {
        PropertyJpaEntity entity = new PropertyJpaEntity();
        entity.setId(domain.getId().getValue());
        entity.setHostId(domain.getHostId());
        entity.setTitle(domain.getTitle());
        entity.setDescription(domain.getDescription());
        entity.setPriceAmount(domain.getPricePerNight().amount());
        entity.setPriceCurrency(domain.getPricePerNight().currency().getCurrencyCode());
        entity.setCountry(domain.getLocation().country());
        entity.setCity(domain.getLocation().city());
        entity.setAddress(domain.getLocation().address());
        entity.setPostalCode(domain.getLocation().postalCode());

        // Convert List<String> to Set<AmenityJpaEntity>
        if (domain.getAmenities() != null && !domain.getAmenities().isEmpty()) {
            Set<AmenityJpaEntity> amenityEntities = new HashSet<>(
                amenityRepository.findByNameIn(domain.getAmenities())
            );
            entity.setAmenities(amenityEntities);
        }

        entity.setBedrooms(domain.getSpecs().bedrooms());
        entity.setBathrooms(domain.getSpecs().bathrooms());
        entity.setMaxGuests(domain.getSpecs().maxGuests());
        entity.setPropertyType(domain.getPropertyType().name());
        entity.setStatus(domain.getStatus().name());
        entity.setCoverImageId(domain.getCoverImageId());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(domain.getUpdatedAt());
        entity.setPublishedAt(domain.getPublishedAt());

        List<PropertyImageJpaEntity> imageEntities = domain.getImages().stream()
            .map(img -> toImageEntity(img, entity))
            .collect(Collectors.toList());
        entity.setImages(imageEntities);

        List<PropertyVideoJpaEntity> videoEntities = domain.getVideos().stream()
            .map(vid -> toVideoEntity(vid, entity))
            .collect(Collectors.toList());
        entity.setVideos(videoEntities);

        return entity;
    }

    private PropertyImage toImageDomain(PropertyImageJpaEntity entity) {
        return PropertyImage.of(
            entity.getId(),
            PropertyId.of(entity.getProperty().getId()),
            entity.getUrl(),
            entity.getDisplayOrder(),
            entity.getUploadedAt()
        );
    }

    private PropertyVideo toVideoDomain(PropertyVideoJpaEntity entity) {
        return PropertyVideo.of(
            entity.getId(),
            PropertyId.of(entity.getProperty().getId()),
            entity.getUrl(),
            entity.getThumbnailUrl(),
            entity.getDisplayOrder(),
            entity.getDurationSeconds(),
            entity.getUploadedAt()
        );
    }

    private PropertyImageJpaEntity toImageEntity(PropertyImage domain, PropertyJpaEntity property) {
        PropertyImageJpaEntity entity = new PropertyImageJpaEntity();
        entity.setId(domain.getId());
        entity.setProperty(property);
        entity.setUrl(domain.getUrl());
        entity.setDisplayOrder(domain.getDisplayOrder());
        entity.setUploadedAt(domain.getUploadedAt());
        return entity;
    }

    private PropertyVideoJpaEntity toVideoEntity(PropertyVideo domain, PropertyJpaEntity property) {
        PropertyVideoJpaEntity entity = new PropertyVideoJpaEntity();
        entity.setId(domain.getId());
        entity.setProperty(property);
        entity.setUrl(domain.getUrl());
        entity.setThumbnailUrl(domain.getThumbnailUrl());
        entity.setDisplayOrder(domain.getDisplayOrder());
        entity.setDurationSeconds(domain.getDurationSeconds());
        entity.setUploadedAt(domain.getUploadedAt());
        return entity;
    }
}
