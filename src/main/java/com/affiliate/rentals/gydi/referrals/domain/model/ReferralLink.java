package com.affiliate.rentals.gydi.referrals.domain.model;

import java.time.LocalDateTime;

/**
 * Modelo de dominio para enlace de referido
 *
 * Representa un enlace único generado por un afiliado para promocionar
 * una propiedad y ganar comisiones por conversiones.
 *
 * Este es un modelo rico de dominio con lógica de negocio y sin
 * dependencias de infraestructura.
 */
public class ReferralLink {

    private Long id;
    private Long affiliateId;
    private Long propertyId;
    private String encryptedToken;
    private String shortCode;
    private Integer clicksCount;
    private ReferralLinkStatus status;
    private LocalDateTime expiresAt;
    private LocalDateTime deletedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Constructor privado - usar builder o métodos de fábrica
    private ReferralLink() {
        this.clicksCount = 0;
        this.status = ReferralLinkStatus.ACTIVE;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Método de fábrica para crear un nuevo enlace de referido
     */
    public static ReferralLink create(Long affiliateId, Long propertyId,
            String encryptedToken, String shortCode,
            LocalDateTime expiresAt) {
        if (affiliateId == null || affiliateId <= 0) {
            throw new IllegalArgumentException("AffiliateId must be positive");
        }
        if (propertyId == null) {
            throw new IllegalArgumentException("PropertyId cannot be null");
        }
        if (encryptedToken == null || encryptedToken.isBlank()) {
            throw new IllegalArgumentException("EncryptedToken cannot be empty");
        }
        if (expiresAt == null || expiresAt.isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("ExpiresAt must be in the future");
        }

        ReferralLink link = new ReferralLink();

        link.affiliateId = affiliateId;
        link.propertyId = propertyId;
        link.encryptedToken = encryptedToken;
        link.shortCode = shortCode;
        link.expiresAt = expiresAt;

        return link;
    }

    /**
     * Verifica si el enlace está activo y puede ser usado
     */
    public boolean isActive() {
        return status == ReferralLinkStatus.ACTIVE
                && deletedAt == null
                && !isExpired();
    }

    /**
     * Verifica si el enlace ha expirado
     */
    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }

    /**
     * Incrementa el contador de clicks
     */
    public void incrementClicks() {
        if (!isActive()) {
            throw new IllegalStateException("Cannot track clicks on inactive link");
        }
        this.clicksCount++;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Desactiva el enlace
     */
    public void deactivate() {
        if (status == ReferralLinkStatus.DISABLED) {
            throw new IllegalStateException("Cannot deactivate a deleted link");
        }
        this.status = ReferralLinkStatus.PAUSED;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Reactiva el enlace
     */
    public void activate() {
        if (status == ReferralLinkStatus.DISABLED) {
            throw new IllegalStateException("Cannot activate a deleted link");
        }
        if (isExpired()) {
            throw new IllegalStateException("Cannot activate an expired link");
        }
        this.status = ReferralLinkStatus.ACTIVE;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Marca el enlace como expirado
     */
    public void markAsExpired() {
        if (status != ReferralLinkStatus.DISABLED) {
            this.status = ReferralLinkStatus.EXPIRED;
            this.updatedAt = LocalDateTime.now();
        }
    }

    /**
     * Soft delete del enlace
     */
    public void delete() {
        this.status = ReferralLinkStatus.DISABLED;
        this.deletedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Extiende la fecha de expiración
     */
    public void extendExpiration(LocalDateTime newExpiresAt) {
        if (newExpiresAt == null || newExpiresAt.isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("New expiration date must be in the future");
        }
        if (status == ReferralLinkStatus.DISABLED) {
            throw new IllegalStateException("Cannot extend expiration of a deleted link");
        }

        this.expiresAt = newExpiresAt;

        // Si estaba expirado, reactivar
        if (status == ReferralLinkStatus.EXPIRED) {
            this.status = ReferralLinkStatus.ACTIVE;
        }

        this.updatedAt = LocalDateTime.now();
    }

    // Getters
    public Long getId() {
        return id;
    }

    public Long getAffiliateId() {
        return affiliateId;
    }

    public Long getPropertyId() {
        return propertyId;
    }

    public String getEncryptedToken() {
        return encryptedToken;
    }

    public String getShortCode() {
        return shortCode;
    }

    public Integer getClicksCount() {
        return clicksCount;
    }

    public ReferralLinkStatus getStatus() {
        return status;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    // Setters para reconstrucción desde persistencia
    public void setId(Long id) {
        this.id = id;
    }

    public void setClicksCount(Integer clicksCount) {
        this.clicksCount = clicksCount;
    }

    public void setStatus(ReferralLinkStatus status) {
        this.status = status;
    }

    public void setDeletedAt(LocalDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}