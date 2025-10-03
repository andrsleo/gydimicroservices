package com.affiliate.rentals.properties.adapters.out.persistence;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.affiliate.rentals.properties.adapters.out.persistence.entity.PropertyEntity;

@Repository
public interface SpringDataPropertyRepository extends JpaRepository<PropertyEntity, UUID> {
    // Podemos agregar query methods si son necesarios en el futuro (ej: findByOwnerId)
}
