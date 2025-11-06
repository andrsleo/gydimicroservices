package com.affiliate.rentals.gydi.users.infrastructure.out.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * JPA entity representing a user profile in the users schema.
 *
 * <p>This entity maps to the {@code user_profile} table in the {@code users} schema.
 * It stores extended user information with a 1:1 relationship to UserEntity.
 * Uses PostgreSQL JSONB for flexible storage of social links, preferences, and metadata.</p>
 *
 * <p>This is an infrastructure-layer class following hexagonal architecture principles,
 * utilizing Java 21 features and modern JPA patterns.</p>
 *
 * @author GYDI Development Team
 * @see UserEntity
 */
@Entity
@Table(name = "user_profile", schema = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileEntity {

    /**
     * The unique identifier for this user profile.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * Foreign key reference to the associated user (1:1 relationship).
     */
    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    /**
     * The user's first name.
     */
    @Column(name = "first_name", length = 100)
    private String firstName;

    /**
     * The user's last name.
     */
    @Column(name = "last_name", length = 100)
    private String lastName;

    /**
     * The user's date of birth.
     */
    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    /**
     * The user's gender.
     */
    @Column(name = "gender", length = 20)
    private String gender;

    /**
     * The user's biography or description.
     */
    @Column(name = "bio", columnDefinition = "TEXT")
    private String bio;

    /**
     * The user's phone number in E.164 format.
     */
    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    /**
     * Full country name (e.g., "United States", "Spain").
     */
    @Column(name = "country", length = 100)
    private String country;

    /**
     * The user's city of residence.
     */
    @Column(name = "city", length = 100)
    private String city;

    /**
     * The user's street address.
     */
    @Column(name = "address", length = 500)
    private String address;

    /**
     * The user's postal/ZIP code.
     */
    @Column(name = "postal_code", length = 20)
    private String postalCode;

    /**
     * ISO 639-1 language code (e.g., "en", "es").
     */
    @Column(name = "preferred_language", length = 5)
    private String preferredLanguage;

    /**
     * URL to the user's cover image.
     */
    @Column(name = "cover_image_url", length = 500)
    private String coverImageUrl;

    /**
     * URL to the user's personal or business website.
     */
    @Column(name = "website_url", length = 500)
    private String websiteUrl;

    /**
     * Social media links stored as JSONB.
     * Example: {"linkedin": "url", "twitter": "@handle"}
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "social_links", columnDefinition = "jsonb")
    private Map<String, String> socialLinks;

    /**
     * User preferences stored as JSONB.
     * Example: {"theme": "dark", "notifications": {...}}
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "preferences", columnDefinition = "jsonb")
    private Map<String, Object> preferences;

    /**
     * Profile visibility level (public, private, connections).
     */
    @Column(name = "profile_visibility", length = 20)
    private String profileVisibility;

    /**
     * Whether email notifications are enabled.
     */
    @Column(name = "email_notifications_enabled")
    private Boolean emailNotificationsEnabled;

    /**
     * Whether SMS notifications are enabled.
     */
    @Column(name = "sms_notifications_enabled")
    private Boolean smsNotificationsEnabled;

    /**
     * Extensible metadata stored as JSONB.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb")
    private Map<String, Object> metadata;

    /**
     * The timestamp when this profile was created.
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * The timestamp when this profile was last updated.
     */
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Sets timestamps before persisting.
     */
    @PrePersist
    protected void onCreate() {
        var now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
    }

    /**
     * Updates the updated_at timestamp before updating.
     */
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserProfileEntity that = (UserProfileEntity) o;
        return Objects.equals(id, that.id) && Objects.equals(userId, that.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, userId);
    }

    @Override
    public String toString() {
        return "UserProfileEntity{id=%s, userId=%s, gender=%s, profileVisibility=%s}"
                .formatted(id, userId, gender, profileVisibility);
    }
}