package com.affiliate.rentals.gydi.properties.infrastructure.out.persistence.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.affiliate.rentals.gydi.users.infrastructure.out.persistence.entity.UserEntity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "properties", schema = "properties")
public class PropertyJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "host_id", nullable = false)
    private UserEntity host;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(length = 255, unique = true)
    private String slug; // SEO-friendly URL slug

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "price_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal priceAmount;

    @Column(name = "price_currency", nullable = false, length = 10)
    private String priceCurrency;

    @Column(name = "sale_price_amount", precision = 15, scale = 2)
    private BigDecimal salePriceAmount;

    @Column(name = "sale_price_currency", length = 10)
    private String salePriceCurrency;

    @Column(nullable = false, length = 100)
    private String country;

    @Column(nullable = false, length = 100)
    private String city;

    @Column(length = 255)
    private String address;

    @Column(name = "postal_code", length = 20)
    private String postalCode;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "property_amenities", schema = "properties", joinColumns = @JoinColumn(name = "property_id"), inverseJoinColumns = @JoinColumn(name = "amenity_id"))
    @BatchSize(size = 10)
    private Set<AmenityJpaEntity> amenities = new HashSet<>();

    @Column(nullable = false)
    private Integer bedrooms;

    @Column(nullable = false)
    private Integer bathrooms;

    @Column(name = "max_guests", nullable = false)
    private Integer maxGuests;

    @Column(name = "property_type", nullable = false, length = 50)
    private String propertyType;

    @Column(name = "listing_type", nullable = false, length = 20)
    private String listingType;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "cover_image_id")
    private Long coverImageId;

    // Admin approval workflow fields
    @Column(name = "denial_reason", columnDefinition = "TEXT")
    private String denialReason;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "denied_at")
    private LocalDateTime deniedAt;

    // Airbnb import fields
    @Column(name = "airbnb_url", length = 500)
    private String airbnbUrl;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "import_mode", nullable = false, length = 50)
    private ImportMode importMode = ImportMode.MANUAL;

    @Column(name = "imported_at")
    private LocalDateTime importedAt;

    @Column(name = "airbnb_listing_id", length = 100)
    private String airbnbListingId;

    @Column(name = "ical_url_airbnb", length = 500)
    private String icalUrlAirbnb;

    @OneToMany(mappedBy = "property", cascade = CascadeType.ALL, orphanRemoval = true)
    @BatchSize(size = 10)
    @OrderBy("displayOrder ASC")
    private List<PropertyImageJpaEntity> images = new ArrayList<>();

    @OneToMany(mappedBy = "property", cascade = CascadeType.ALL, orphanRemoval = true)
    @BatchSize(size = 10)
    @OrderBy("displayOrder ASC")
    private List<PropertyVideoJpaEntity> videos = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

}
