package com.affiliate.rentals.gydi.properties.application.mapper;

import com.affiliate.rentals.gydi.properties.application.dto.*;
import com.affiliate.rentals.gydi.properties.domain.model.*;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Manual mapper for Property domain model and DTOs.
 * Replaces MapStruct to avoid classloader issues during clean builds.
 */
@Component
public class PropertyMapper {

    public PropertyResponse toPropertyResponse(Property property) {
        if (property == null)
            return null;

        return new PropertyResponse(
                property.getId().toString(),
                property.getHostId(),
                property.getTitle(),
                property.getSlug(),
                property.getDescription(),
                property.getPricePerNight().amount(),
                property.getPricePerNight().currency().getCurrencyCode(),
                property.getSalePrice() != null ? property.getSalePrice().amount() : null,
                property.getLocation().country(),
                property.getLocation().city(),
                property.getSpecs().bedrooms(),
                property.getSpecs().bathrooms(),
                property.getSpecs().maxGuests(),
                property.getPropertyType().name(),
                property.getListingType().name(),
                property.getStatus().name(),
                property.getImageCount(),
                property.getVideoCount(),
                getMainImageUrl(property),
                property.getImportMode() != null ? property.getImportMode().name() : null,
                property.getCreatedAt(),
                property.getPublishedAt(),
                property.getAirbnbUrl(),
                property.getIcalUrlAirbnb(),
                property.getDenialReason(),
                property.getSubmittedAt(),
                property.getApprovedAt());
    }

    public PropertyDetailResponse toPropertyDetailResponse(Property property) {
        if (property == null)
            return null;

        return new PropertyDetailResponse(
                property.getId().toString(),
                property.getHostId(),
                property.getTitle(),
                property.getSlug(),
                property.getDescription(),
                property.getPricePerNight().amount(),
                property.getPricePerNight().currency().getCurrencyCode(),
                property.getSalePrice() != null ? property.getSalePrice().amount() : null,
                toLocationDTO(property.getLocation()),
                property.getAmenities(),
                toSpecsDTO(property.getSpecs()),
                property.getPropertyType().name(),
                property.getListingType().name(),
                property.getStatus().name(),
                toImageDTOList(property.getImages()),
                toVideoDTOList(property.getVideos()),
                property.getCoverImageId() != null ? property.getCoverImageId().toString() : null,
                property.getImageCount(),
                property.getVideoCount(),
                property.getAirbnbUrl(),
                property.getImportMode() != null ? property.getImportMode().name() : null,
                property.getImportedAt(),
                property.getAirbnbListingId(),
                property.getIcalUrlAirbnb(),
                property.getCreatedAt(),
                property.getUpdatedAt(),
                property.getPublishedAt(),
                property.getDenialReason(),
                property.getSubmittedAt(),
                property.getApprovedAt());
    }

    public List<PropertyResponse> toPropertyResponseList(List<Property> properties) {
        if (properties == null)
            return null;
        return properties.stream()
                .map(this::toPropertyResponse)
                .collect(Collectors.toList());
    }

    public PropertyLocationDTO toLocationDTO(PropertyLocation location) {
        if (location == null)
            return null;
        return new PropertyLocationDTO(
                location.country(),
                location.city(),
                location.address(),
                location.postalCode());
    }

    public PropertySpecsDTO toSpecsDTO(PropertySpecs specs) {
        if (specs == null)
            return null;
        return new PropertySpecsDTO(
                specs.bedrooms(),
                specs.bathrooms(),
                specs.maxGuests());
    }

    public PropertyImageDTO toImageDTO(PropertyImage image) {
        if (image == null)
            return null;
        return new PropertyImageDTO(
                image.getId().toString(),
                image.getUrl(),
                image.getDisplayOrder(),
                image.getUploadedAt());
    }

    public PropertyVideoDTO toVideoDTO(PropertyVideo video) {
        if (video == null)
            return null;
        return new PropertyVideoDTO(
                video.getId().toString(),
                video.getUrl(),
                video.getThumbnailUrl(),
                video.getDisplayOrder(),
                video.getDurationSeconds(),
                video.getUploadedAt());
    }

    public List<PropertyImageDTO> toImageDTOList(List<PropertyImage> images) {
        if (images == null)
            return null;
        return images.stream()
                .map(this::toImageDTO)
                .collect(Collectors.toList());
    }

    public List<PropertyVideoDTO> toVideoDTOList(List<PropertyVideo> videos) {
        if (videos == null)
            return null;
        return videos.stream()
                .map(this::toVideoDTO)
                .collect(Collectors.toList());
    }

    private String getMainImageUrl(Property property) {
        // First, try to get the cover image
        if (property.getCoverImageId() != null) {
            return property.getImages().stream()
                    .filter(img -> img.getId().equals(property.getCoverImageId()))
                    .findFirst()
                    .map(PropertyImage::getUrl)
                    .orElse(null);
        }

        // If no cover image, return the first image (already ordered by displayOrder)
        return property.getImages().stream()
                .findFirst()
                .map(PropertyImage::getUrl)
                .orElse(null);
    }
}
