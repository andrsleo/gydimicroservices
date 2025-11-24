package com.affiliate.rentals.gydi.referrals.domain.model;

import java.time.LocalDateTime;

/**
 * Modelo de dominio para tracking de clicks en enlaces de referido
 *
 * Almacena información anonimizada (hashes) de cada click en un enlace
 * de referido, cumpliendo con regulaciones de privacidad (GDPR).
 *
 * Los datos PII (IP, User-Agent) se almacenan como hashes SHA-256
 * irreversibles.
 */
public class ReferralClick {

    private Long id;
    private Long referralLinkId;
    private byte[] ipAddressHash; // SHA-256(IP + salt)
    private byte[] userAgentHash; // SHA-256(UserAgent + salt)
    private String fingerprint;
    private String countryCode;
    private DeviceType deviceType;
    private Integer botScore; // 0-100: mayor = más probable que sea bot
    private LocalDateTime clickedAt;

    // Constructor privado
    private ReferralClick() {
        this.deviceType = DeviceType.UNKNOWN;
        this.clickedAt = LocalDateTime.now();
    }

    /**
     * Método de fábrica para crear un nuevo click
     */
    public static ReferralClick create(Long referralLinkId,
            byte[] ipAddressHash,
            byte[] userAgentHash,
            String fingerprint,
            String countryCode,
            DeviceType deviceType,
            Integer botScore) {
        if (referralLinkId == null) {
            throw new IllegalArgumentException("ReferralLinkId cannot be null");
        }
        if (ipAddressHash == null || ipAddressHash.length == 0) {
            throw new IllegalArgumentException("IP address hash cannot be empty");
        }
        if (userAgentHash == null || userAgentHash.length == 0) {
            throw new IllegalArgumentException("User agent hash cannot be empty");
        }

        ReferralClick click = new ReferralClick();

        click.referralLinkId = referralLinkId;
        click.ipAddressHash = ipAddressHash;
        click.userAgentHash = userAgentHash;
        click.fingerprint = fingerprint;
        click.countryCode = countryCode;
        click.deviceType = deviceType != null ? deviceType : DeviceType.UNKNOWN;
        click.botScore = botScore;

        return click;
    }

    /**
     * Verifica si el click probablemente proviene de un bot
     */
    public boolean isProbablyBot() {
        return botScore != null && botScore >= 70;
    }

    /**
     * Verifica si el click es de alta confianza (no es bot)
     */
    public boolean isHighConfidenceHuman() {
        return botScore != null && botScore < 30;
    }

    /**
     * Obtiene el nivel de riesgo del click
     */
    public String getRiskLevel() {
        if (botScore == null) {
            return "UNKNOWN";
        }
        if (botScore >= 80) {
            return "VERY_HIGH";
        }
        if (botScore >= 60) {
            return "HIGH";
        }
        if (botScore >= 40) {
            return "MEDIUM";
        }
        if (botScore >= 20) {
            return "LOW";
        }
        return "VERY_LOW";
    }

    // Getters
    public Long getId() {
        return id;
    }

    public Long getReferralLinkId() {
        return referralLinkId;
    }

    public byte[] getIpAddressHash() {
        return ipAddressHash;
    }

    public byte[] getUserAgentHash() {
        return userAgentHash;
    }

    public String getFingerprint() {
        return fingerprint;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public DeviceType getDeviceType() {
        return deviceType;
    }

    public Integer getBotScore() {
        return botScore;
    }

    public LocalDateTime getClickedAt() {
        return clickedAt;
    }

    // Setters para reconstrucción desde persistencia
    public void setId(Long id) {
        this.id = id;
    }

    public void setClickedAt(LocalDateTime clickedAt) {
        this.clickedAt = clickedAt;
    }
}