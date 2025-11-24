package com.affiliate.rentals.gydi.properties.infrastructure.out.persistence.adapter;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import com.affiliate.rentals.gydi.properties.domain.model.Property;
import com.affiliate.rentals.gydi.properties.domain.model.PropertyId;
import com.affiliate.rentals.gydi.properties.domain.model.PropertyStatus;
import com.affiliate.rentals.gydi.properties.domain.ports.out.PropertyRepositoryPort;
import com.affiliate.rentals.gydi.properties.infrastructure.out.persistence.entity.PropertyJpaEntity;
import com.affiliate.rentals.gydi.properties.infrastructure.out.persistence.mapper.PropertyJpaMapper;
import com.affiliate.rentals.gydi.properties.infrastructure.out.persistence.repository.PropertyJpaRepository;

import jakarta.persistence.criteria.Predicate;

@Component
public class PropertyRepositoryAdapter implements PropertyRepositoryPort {

    private final PropertyJpaRepository jpaRepository;
    private final PropertyJpaMapper mapper;

    public PropertyRepositoryAdapter(PropertyJpaRepository jpaRepository, PropertyJpaMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Property save(Property property) {
        PropertyJpaEntity entity = mapper.toEntity(property);
        PropertyJpaEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<Property> findById(PropertyId id) {
        return jpaRepository.findById(id.getValue())
                .map(mapper::toDomain);
    }

    @Override
    public Optional<Property> findBySlug(String slug) {
        return jpaRepository.findBySlug(slug)
                .map(mapper::toDomain);
    }

    @Override
    public List<Property> findByHostId(Long hostId) {
        return jpaRepository.findByHostId(hostId).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public PropertySearchResult findAll(PropertySearchSpec spec) {
        Specification<PropertyJpaEntity> specification = buildSpecification(spec);
        
        Sort sort = Sort.by(
            "DESC".equalsIgnoreCase(spec.sortDirection()) ? Sort.Direction.DESC : Sort.Direction.ASC,
            spec.sortBy() != null ? spec.sortBy() : "createdAt"
        );
        
        Pageable pageable = PageRequest.of(spec.page(), spec.size(), sort);
        Page<PropertyJpaEntity> page = jpaRepository.findAll(specification, pageable);
        
        List<Property> properties = page.getContent().stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
        
        return new PropertySearchResult(
            properties,
            page.getTotalElements(),
            page.getTotalPages(),
            page.getNumber(),
            page.getSize()
        );
    }

    @Override
    public void deleteById(PropertyId id) {
        jpaRepository.deleteById(id.getValue());
    }

    @Override
    public boolean existsById(PropertyId id) {
        return jpaRepository.existsById(id.getValue());
    }

    private Specification<PropertyJpaEntity> buildSpecification(PropertySearchSpec spec) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (spec.status() != null) {
                predicates.add(cb.equal(root.get("status"), spec.status().name()));
            }
            
            if (spec.propertyType() != null) {
                predicates.add(cb.equal(root.get("propertyType"), spec.propertyType().name()));
            }

            if (spec.listingType() != null && !spec.listingType().isBlank()) {
                predicates.add(cb.equal(root.get("listingType"), spec.listingType()));
            }

            if (spec.country() != null) {
                predicates.add(cb.equal(root.get("country"), spec.country()));
            }
            
            if (spec.city() != null) {
                predicates.add(cb.equal(root.get("city"), spec.city()));
            }
            
            if (spec.minPrice() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("priceAmount"), spec.minPrice()));
            }
            
            if (spec.maxPrice() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("priceAmount"), spec.maxPrice()));
            }
            
            if (spec.minBedrooms() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("bedrooms"), spec.minBedrooms()));
            }
            
            if (spec.minBathrooms() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("bathrooms"), spec.minBathrooms()));
            }
            
            if (spec.minGuests() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("maxGuests"), spec.minGuests()));
            }
            
            if (spec.searchText() != null && !spec.searchText().isBlank()) {
                String searchPattern = "%" + spec.searchText().toLowerCase() + "%";
                predicates.add(cb.or(
                    cb.like(cb.lower(root.get("title")), searchPattern),
                    cb.like(cb.lower(root.get("description")), searchPattern)
                ));
            }

            // Filter by amenities (ALL must be present - AND logic)
            if (spec.amenities() != null && !spec.amenities().isEmpty()) {
                var amenitiesJoin = root.join("amenities");
                Predicate amenitiesPredicate = amenitiesJoin.get("name").in(spec.amenities());
                predicates.add(amenitiesPredicate);

                // Group by property ID and count must match the number of requested amenities
                query.groupBy(root.get("id"));
                query.having(cb.equal(cb.countDistinct(amenitiesJoin), spec.amenities().size()));
            }

            // Exclude deleted properties (soft delete via status)
            predicates.add(cb.notEqual(root.get("status"), PropertyStatus.DELETED.name()));

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
