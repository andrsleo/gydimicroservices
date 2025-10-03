package com.affiliate.rentals.properties.adapters.out.persistence.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "properties")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // requerido por JPA
@AllArgsConstructor
@Builder
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class PropertyEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @EqualsAndHashCode.Include
    @Column(name = "id", columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "owner_id", nullable = false, columnDefinition = "uuid")
    private UUID ownerId;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Column(name = "location", nullable = false, length = 200)
    private String location;

    @Column(name = "price_per_night", precision = 12, scale = 2, nullable = false)
    private BigDecimal pricePerNight;

    @Builder.Default
    @Column(name = "currency", nullable = false, length = 10)
    private String currency = "COP";

    @Builder.Default
    @Column(name = "bedrooms")
    private Integer bedrooms = 0;

    @Builder.Default
    @Column(name = "bathrooms")
    private Integer bathrooms = 0;

    @Builder.Default
    @Column(name = "beds")
    private Integer beds = 0;

    @Builder.Default
    @Column(name = "capacity")
    private Integer capacity = 1;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "principal_image", nullable = false, length = 500)
    private String principalImage;
}

