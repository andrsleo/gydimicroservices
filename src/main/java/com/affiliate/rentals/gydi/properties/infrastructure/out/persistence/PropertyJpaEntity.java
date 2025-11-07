package com.affiliate.rentals.gydi.properties.infrastructure.out.persistence;

import jakarta.persistence.*;
import org.hibernate.annotations.BatchSize;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "properties", schema = "properties")
public class PropertyJpaEntity {

    @Id
    private UUID id;

    @Column(name = "host_id", nullable = false)
    private Long hostId;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "price_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal priceAmount;

    @Column(name = "price_currency", nullable = false, length = 3)
    private String priceCurrency;

    @Column(name = "sale_price", precision = 15, scale = 2)
    private BigDecimal salePrice;

    @Column(nullable = false, length = 100)
    private String country;

    @Column(nullable = false, length = 100)
    private String city;

    @Column(length = 255)
    private String address;

    @Column(name = "postal_code", length = 20)
    private String postalCode;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "property_amenities",
        schema = "properties",
        joinColumns = @JoinColumn(name = "property_id"),
        inverseJoinColumns = @JoinColumn(name = "amenity_id")
    )
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
    private UUID coverImageId;

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

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public Long getHostId() { return hostId; }
    public void setHostId(Long hostId) { this.hostId = hostId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public BigDecimal getPriceAmount() { return priceAmount; }
    public void setPriceAmount(BigDecimal priceAmount) { this.priceAmount = priceAmount; }

    public String getPriceCurrency() { return priceCurrency; }
    public void setPriceCurrency(String priceCurrency) { this.priceCurrency = priceCurrency; }

    public BigDecimal getSalePrice() { return salePrice; }
    public void setSalePrice(BigDecimal salePrice) { this.salePrice = salePrice; }

    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getPostalCode() { return postalCode; }
    public void setPostalCode(String postalCode) { this.postalCode = postalCode; }

    public Set<AmenityJpaEntity> getAmenities() { return amenities; }
    public void setAmenities(Set<AmenityJpaEntity> amenities) { this.amenities = amenities; }

    public Integer getBedrooms() { return bedrooms; }
    public void setBedrooms(Integer bedrooms) { this.bedrooms = bedrooms; }

    public Integer getBathrooms() { return bathrooms; }
    public void setBathrooms(Integer bathrooms) { this.bathrooms = bathrooms; }

    public Integer getMaxGuests() { return maxGuests; }
    public void setMaxGuests(Integer maxGuests) { this.maxGuests = maxGuests; }

    public String getPropertyType() { return propertyType; }
    public void setPropertyType(String propertyType) { this.propertyType = propertyType; }

    public String getListingType() { return listingType; }
    public void setListingType(String listingType) { this.listingType = listingType; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public LocalDateTime getPublishedAt() { return publishedAt; }
    public void setPublishedAt(LocalDateTime publishedAt) { this.publishedAt = publishedAt; }

    public UUID getCoverImageId() { return coverImageId; }
    public void setCoverImageId(UUID coverImageId) { this.coverImageId = coverImageId; }

    public List<PropertyImageJpaEntity> getImages() { return images; }
    public void setImages(List<PropertyImageJpaEntity> images) { this.images = images; }

    public List<PropertyVideoJpaEntity> getVideos() { return videos; }
    public void setVideos(List<PropertyVideoJpaEntity> videos) { this.videos = videos; }
}
