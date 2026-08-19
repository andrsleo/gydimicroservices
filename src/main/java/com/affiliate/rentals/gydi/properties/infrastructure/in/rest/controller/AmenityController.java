package com.affiliate.rentals.gydi.properties.infrastructure.in.rest.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.affiliate.rentals.gydi.properties.infrastructure.out.persistence.repository.AmenityJpaRepository;

/**
 * REST Controller for Amenities
 * Provides endpoints to retrieve available amenities
 */
@RestController
@RequestMapping("/api/amenities")
@CrossOrigin(origins = "*")
public class AmenityController {

    private final AmenityJpaRepository amenityRepository;

    public AmenityController(AmenityJpaRepository amenityRepository) {
        this.amenityRepository = amenityRepository;
    }

    /**
     * Get all available amenities
     *
     * @return List of all amenities
     */
    @GetMapping
    public ResponseEntity<List<AmenityResponse>> getAllAmenities() {
        List<AmenityResponse> amenities = amenityRepository.findAll().stream()
                .map(entity -> new AmenityResponse(
                        entity.getId(),
                        entity.getName(),
                        entity.getDescription(),
                        entity.getCategory()
                ))
                .toList();

        return ResponseEntity.ok(amenities);
    }

    /**
     * Get amenities by category
     *
     * @param category amenity category
     * @return List of amenities in the category
     */
    @GetMapping("/category/{category}")
    public ResponseEntity<List<AmenityResponse>> getAmenitiesByCategory(
            @PathVariable String category
    ) {
        List<AmenityResponse> amenities = amenityRepository.findByCategory(category).stream()
                .map(entity -> new AmenityResponse(
                        entity.getId(),
                        entity.getName(),
                        entity.getDescription(),
                        entity.getCategory()
                ))
                .toList();

        return ResponseEntity.ok(amenities);
    }

    /**
     * Amenity Response DTO
     */
    public record AmenityResponse(
            Integer id,
            String name,
            String description,
            String category
    ) {}
}