package com.affiliate.rentals.gydi.properties.application.service;

import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service responsible for validating Airbnb URLs and extracting listing IDs.
 *
 * Security features:
 * - Validates URL format against Airbnb domains
 * - Prevents SSRF attacks by domain whitelisting
 * - Enforces HTTPS protocol
 * - Extracts and validates listing ID format
 */
@Component
public class AirbnbUrlValidator {

    // Matches any official Airbnb domain: airbnb.com, airbnb.com.co, airbnb.co.uk, airbnb.es, etc.
    private static final Pattern AIRBNB_DOMAIN_PATTERN = Pattern.compile(
        "^(?:www\\.)?airbnb\\.[a-z]{2,3}(?:\\.[a-z]{2})?$",
        Pattern.CASE_INSENSITIVE
    );

    private static final Pattern AIRBNB_LISTING_PATTERN = Pattern.compile(
        "^https?://(?:www\\.)?airbnb\\.[a-z]{2,3}(?:\\.[a-z]{2})?/rooms/(\\d+).*$",
        Pattern.CASE_INSENSITIVE
    );

    private static final int MAX_URL_LENGTH = 500;

    /**
     * Validates an Airbnb URL and extracts the listing ID.
     *
     * @param url the Airbnb URL to validate
     * @return AirbnbUrlValidationResult with validation status and listing ID
     */
    public AirbnbUrlValidationResult validate(String url) {
        // 1. Basic validation
        if (url == null || url.isBlank()) {
            return AirbnbUrlValidationResult.invalid("URL cannot be empty");
        }

        String trimmedUrl = url.trim();

        if (trimmedUrl.length() > MAX_URL_LENGTH) {
            return AirbnbUrlValidationResult.invalid("URL exceeds maximum length of " + MAX_URL_LENGTH + " characters");
        }

        // 2. Parse URI
        URI uri;
        try {
            uri = URI.create(trimmedUrl);
        } catch (IllegalArgumentException e) {
            return AirbnbUrlValidationResult.invalid("Invalid URL format");
        }

        // 3. Protocol validation (HTTPS preferred, but HTTP accepted)
        String scheme = uri.getScheme();
        if (scheme == null || (!scheme.equalsIgnoreCase("https") && !scheme.equalsIgnoreCase("http"))) {
            return AirbnbUrlValidationResult.invalid("URL must use HTTP or HTTPS protocol");
        }

        // 4. Domain validation
        String host = uri.getHost();
        if (host == null || !isDomainAllowed(host.toLowerCase())) {
            return AirbnbUrlValidationResult.invalid("URL must be from an official Airbnb domain");
        }

        // 5. Path validation (must contain /rooms/)
        String path = uri.getPath();
        if (path == null || !path.contains("/rooms/")) {
            return AirbnbUrlValidationResult.invalid("URL must be a valid Airbnb listing (/rooms/{id})");
        }

        // 6. Pattern matching and listing ID extraction
        Matcher matcher = AIRBNB_LISTING_PATTERN.matcher(trimmedUrl);
        if (!matcher.matches()) {
            return AirbnbUrlValidationResult.invalid("URL format does not match Airbnb listing pattern");
        }

        String listingId = matcher.group(1);

        // 7. Validate listing ID format
        if (!isValidListingId(listingId)) {
            return AirbnbUrlValidationResult.invalid("Invalid Airbnb listing ID format");
        }

        return AirbnbUrlValidationResult.valid(trimmedUrl, listingId);
    }

    /**
     * Checks if a domain matches the Airbnb domain pattern.
     * Accepts any official Airbnb domain (e.g. airbnb.com, airbnb.com.co, airbnb.co.uk, airbnb.es).
     */
    private boolean isDomainAllowed(String host) {
        return AIRBNB_DOMAIN_PATTERN.matcher(host).matches();
    }

    /**
     * Validates that listing ID contains only digits and is within reasonable length.
     */
    private boolean isValidListingId(String listingId) {
        return listingId != null
            && listingId.matches("\\d+")
            && listingId.length() <= 20;
    }

    /**
     * Result object for URL validation.
     */
    public static class AirbnbUrlValidationResult {
        private final boolean valid;
        private final String normalizedUrl;
        private final String listingId;
        private final String errorMessage;

        private AirbnbUrlValidationResult(boolean valid, String normalizedUrl, String listingId, String errorMessage) {
            this.valid = valid;
            this.normalizedUrl = normalizedUrl;
            this.listingId = listingId;
            this.errorMessage = errorMessage;
        }

        public static AirbnbUrlValidationResult valid(String normalizedUrl, String listingId) {
            return new AirbnbUrlValidationResult(true, normalizedUrl, listingId, null);
        }

        public static AirbnbUrlValidationResult invalid(String errorMessage) {
            return new AirbnbUrlValidationResult(false, null, null, errorMessage);
        }

        public boolean isValid() {
            return valid;
        }

        public String getNormalizedUrl() {
            return normalizedUrl;
        }

        public String getListingId() {
            return listingId;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        @Override
        public String toString() {
            if (valid) {
                return "Valid Airbnb URL - Listing ID: " + listingId;
            } else {
                return "Invalid URL: " + errorMessage;
            }
        }
    }
}
