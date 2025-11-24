package com.affiliate.rentals.gydi.users.domain.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;

/**
 * UserProfile domain entity representing extended user information.
 *
 * <p>
 * This immutable domain entity follows Domain-Driven Design (DDD) principles
 * and contains profile-specific information that complements the core User
 * entity.
 * It maintains a 1:1 relationship with User through userId.
 * </p>
 *
 * <p>
 * The entity uses Java 21 features including records for value objects and
 * sealed types for type safety. All data is immutable to ensure thread safety
 * and consistency.
 * </p>
 *
 * <p>
 * Example usage:
 * </p>
 * 
 * <pre>{@code
 * UserProfile profile = UserProfile.builder()
 *         .userId(userId)
 *         .dateOfBirth(LocalDate.of(1990, 1, 1))
 *         .gender(Gender.MALE)
 *         .bio("Software engineer passionate about clean code")
 *         .build();
 * }</pre>
 *
 * @author GYDI Development Team
 */
public final class UserProfile {

    private final Long id;
    private final Long userId;
    private final String firstName;
    private final String lastName;
    private final LocalDate dateOfBirth;
    private final Gender gender;
    private final String bio;
    private final String phoneNumber;
    private final String country;
    private final String city;
    private final String address;
    private final String postalCode;
    private final String preferredLanguage;
    private final String coverImageUrl;
    private final String websiteUrl;
    private final Map<String, String> socialLinks;
    private final Map<String, Object> preferences;
    private final ProfileVisibility profileVisibility;
    private final boolean emailNotificationsEnabled;
    private final boolean smsNotificationsEnabled;
    private final Map<String, Object> metadata;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    /**
     * Private constructor used by the Builder.
     *
     * @param builder the builder instance containing all profile data
     */
    private UserProfile(Builder builder) {
        this.id = builder.id;
        this.userId = Objects.requireNonNull(builder.userId, "User ID cannot be null");
        this.firstName = builder.firstName;
        this.lastName = builder.lastName;
        this.dateOfBirth = builder.dateOfBirth;
        this.gender = builder.gender;
        this.bio = builder.bio;
        this.phoneNumber = builder.phoneNumber;
        this.country = builder.country;
        this.city = builder.city;
        this.address = builder.address;
        this.postalCode = builder.postalCode;
        this.preferredLanguage = Objects.requireNonNullElse(builder.preferredLanguage, "en");
        this.coverImageUrl = builder.coverImageUrl;
        this.websiteUrl = builder.websiteUrl;
        this.socialLinks = builder.socialLinks != null ? Map.copyOf(builder.socialLinks) : Map.of();
        this.preferences = builder.preferences != null ? Map.copyOf(builder.preferences) : Map.of();
        this.profileVisibility = Objects.requireNonNullElse(builder.profileVisibility, ProfileVisibility.PUBLIC);
        this.emailNotificationsEnabled = builder.emailNotificationsEnabled;
        this.smsNotificationsEnabled = builder.smsNotificationsEnabled;
        this.metadata = builder.metadata != null ? Map.copyOf(builder.metadata) : Map.of();
        this.createdAt = Objects.requireNonNullElseGet(builder.createdAt, LocalDateTime::now);
        this.updatedAt = Objects.requireNonNullElseGet(builder.updatedAt, LocalDateTime::now);

        validateDateOfBirth();
    }

    /**
     * Validates that date of birth is not in the future.
     *
     * @throws IllegalArgumentException if date of birth is after current date
     */
    private void validateDateOfBirth() {
        if (dateOfBirth != null && dateOfBirth.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Date of birth cannot be in the future");
        }
    }

    public Long id() {
        return id;
    }

    public Long userId() {
        return userId;
    }

    public String firstName() {
        return firstName;
    }

    public String lastName() {
        return lastName;
    }

    public LocalDate dateOfBirth() {
        return dateOfBirth;
    }

    public Gender gender() {
        return gender;
    }

    public String bio() {
        return bio;
    }

    public String phoneNumber() {
        return phoneNumber;
    }

    public String country() {
        return country;
    }

    public String city() {
        return city;
    }

    public String address() {
        return address;
    }

    public String postalCode() {
        return postalCode;
    }

    public String preferredLanguage() {
        return preferredLanguage;
    }

    public String coverImageUrl() {
        return coverImageUrl;
    }

    public String websiteUrl() {
        return websiteUrl;
    }

    public Map<String, String> socialLinks() {
        return socialLinks;
    }

    public Map<String, Object> preferences() {
        return preferences;
    }

    public ProfileVisibility profileVisibility() {
        return profileVisibility;
    }

    public boolean emailNotificationsEnabled() {
        return emailNotificationsEnabled;
    }

    public boolean smsNotificationsEnabled() {
        return smsNotificationsEnabled;
    }

    public Map<String, Object> metadata() {
        return metadata;
    }

    public LocalDateTime createdAt() {
        return createdAt;
    }

    public LocalDateTime updatedAt() {
        return updatedAt;
    }

    /**
     * Creates a new UserProfile with updated bio.
     *
     * @param newBio the new bio text
     * @return a new UserProfile instance with updated bio
     */
    public UserProfile withBio(String newBio) {
        return builder()
                .from(this)
                .bio(newBio)
                .updatedAt(LocalDateTime.now())
                .build();
    }

    /**
     * Creates a new UserProfile with updated preferences.
     *
     * @param newPreferences the new preferences map
     * @return a new UserProfile instance with updated preferences
     */
    public UserProfile withPreferences(Map<String, Object> newPreferences) {
        return builder()
                .from(this)
                .preferences(newPreferences)
                .updatedAt(LocalDateTime.now())
                .build();
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        UserProfile that = (UserProfile) o;
        return Objects.equals(id, that.id) && Objects.equals(userId, that.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, userId);
    }

    @Override
    public String toString() {
        return "UserProfile[id=%s, userId=%s, gender=%s, profileVisibility=%s]"
                .formatted(id, userId, gender, profileVisibility);
    }

    /**
     * Builder class for constructing {@link UserProfile} instances.
     */
    public static final class Builder {
        private Long id;
        private Long userId;
        private String firstName;
        private String lastName;
        private LocalDate dateOfBirth;
        private Gender gender;
        private String bio;
        private String phoneNumber;
        private String country;
        private String city;
        private String address;
        private String postalCode;
        private String preferredLanguage;
        private String coverImageUrl;
        private String websiteUrl;
        private Map<String, String> socialLinks;
        private Map<String, Object> preferences;
        private ProfileVisibility profileVisibility;
        private boolean emailNotificationsEnabled = true;
        private boolean smsNotificationsEnabled = false;
        private Map<String, Object> metadata;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        private Builder() {
        }

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder userId(Long userId) {
            this.userId = userId;
            return this;
        }

        public Builder firstName(String firstName) {
            this.firstName = firstName;
            return this;
        }

        public Builder lastName(String lastName) {
            this.lastName = lastName;
            return this;
        }

        public Builder dateOfBirth(LocalDate dateOfBirth) {
            this.dateOfBirth = dateOfBirth;
            return this;
        }

        public Builder gender(Gender gender) {
            this.gender = gender;
            return this;
        }

        public Builder bio(String bio) {
            this.bio = bio;
            return this;
        }

        public Builder phoneNumber(String phoneNumber) {
            this.phoneNumber = phoneNumber;
            return this;
        }

        public Builder country(String country) {
            this.country = country;
            return this;
        }

        public Builder city(String city) {
            this.city = city;
            return this;
        }

        public Builder address(String address) {
            this.address = address;
            return this;
        }

        public Builder postalCode(String postalCode) {
            this.postalCode = postalCode;
            return this;
        }

        public Builder preferredLanguage(String preferredLanguage) {
            this.preferredLanguage = preferredLanguage;
            return this;
        }

        public Builder coverImageUrl(String coverImageUrl) {
            this.coverImageUrl = coverImageUrl;
            return this;
        }

        public Builder websiteUrl(String websiteUrl) {
            this.websiteUrl = websiteUrl;
            return this;
        }

        public Builder socialLinks(Map<String, String> socialLinks) {
            this.socialLinks = socialLinks;
            return this;
        }

        public Builder preferences(Map<String, Object> preferences) {
            this.preferences = preferences;
            return this;
        }

        public Builder profileVisibility(ProfileVisibility profileVisibility) {
            this.profileVisibility = profileVisibility;
            return this;
        }

        public Builder emailNotificationsEnabled(boolean emailNotificationsEnabled) {
            this.emailNotificationsEnabled = emailNotificationsEnabled;
            return this;
        }

        public Builder smsNotificationsEnabled(boolean smsNotificationsEnabled) {
            this.smsNotificationsEnabled = smsNotificationsEnabled;
            return this;
        }

        public Builder metadata(Map<String, Object> metadata) {
            this.metadata = metadata;
            return this;
        }

        public Builder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder updatedAt(LocalDateTime updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        /**
         * Copies all fields from an existing UserProfile.
         *
         * @param profile the profile to copy from
         * @return this builder instance
         */
        public Builder from(UserProfile profile) {
            this.id = profile.id;
            this.userId = profile.userId;
            this.firstName = profile.firstName;
            this.lastName = profile.lastName;
            this.dateOfBirth = profile.dateOfBirth;
            this.gender = profile.gender;
            this.bio = profile.bio;
            this.phoneNumber = profile.phoneNumber;
            this.country = profile.country;
            this.city = profile.city;
            this.address = profile.address;
            this.postalCode = profile.postalCode;
            this.preferredLanguage = profile.preferredLanguage;
            this.coverImageUrl = profile.coverImageUrl;
            this.websiteUrl = profile.websiteUrl;
            this.socialLinks = profile.socialLinks;
            this.preferences = profile.preferences;
            this.profileVisibility = profile.profileVisibility;
            this.emailNotificationsEnabled = profile.emailNotificationsEnabled;
            this.smsNotificationsEnabled = profile.smsNotificationsEnabled;
            this.metadata = profile.metadata;
            this.createdAt = profile.createdAt;
            this.updatedAt = profile.updatedAt;
            return this;
        }

        public UserProfile build() {
            return new UserProfile(this);
        }
    }
}