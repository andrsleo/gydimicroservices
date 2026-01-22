package com.affiliate.rentals.gydi.shared.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.cloudinary.Cloudinary;

import lombok.extern.slf4j.Slf4j;

/**
 * Cloudinary Configuration
 *
 * <p>
 * Configures Cloudinary client for image and video storage.
 * Only activated when {@code storage.provider=cloudinary} is set.
 * </p>
 *
 * <p>
 * <strong>Configuration:</strong>
 * <pre>
 * cloudinary:
 *   url: cloudinary://api_key:api_secret@cloud_name
 *
 * storage:
 *   provider: cloudinary  # Required to activate this configuration
 * </pre>
 * </p>
 *
 * <p>
 * <strong>Environment Variable:</strong><br>
 * Set {@code CLOUDINARY_URL} in Railway/environment:
 * <pre>
 * CLOUDINARY_URL=***REMOVED***
 * STORAGE_PROVIDER=cloudinary
 * </pre>
 * </p>
 *
 * <p>
 * <strong>Features:</strong>
 * <ul>
 *   <li>Automatic image optimization and transformation</li>
 *   <li>CDN delivery for fast global access</li>
 *   <li>Support for images (JPG, PNG, WEBP, HEIC) and videos (MP4, WEBM, MOV)</li>
 *   <li>Automatic format conversion and quality optimization</li>
 *   <li>Responsive image generation</li>
 * </ul>
 * </p>
 *
 * @see com.affiliate.rentals.gydi.shared.infrastructure.storage.CloudinaryStorageAdapter
 * @author GYDI Development Team
 */
@Slf4j
@Configuration
@ConditionalOnProperty(name = "storage.provider", havingValue = "cloudinary")
public class CloudinaryConfig {

    @Value("${cloudinary.url}")
    private String cloudinaryUrl;

    /**
     * Creates Cloudinary client bean.
     *
     * <p>
     * The Cloudinary URL format is:
     * {@code cloudinary://api_key:api_secret@cloud_name}
     * </p>
     *
     * <p>
     * <strong>Example:</strong><br>
     * {@code ***REMOVED***}
     * </p>
     *
     * @return configured Cloudinary instance
     * @throws IllegalArgumentException if cloudinary.url is invalid
     */
    @Bean
    public Cloudinary cloudinary() {
        // Debug logging
        log.info("CLOUDINARY_URL raw value: [{}]", cloudinaryUrl);
        log.info("CLOUDINARY_URL length: {}", cloudinaryUrl != null ? cloudinaryUrl.length() : "null");
        log.info("CLOUDINARY_URL starts with 'cloudinary://': {}",
            cloudinaryUrl != null && cloudinaryUrl.startsWith("cloudinary://"));

        if (cloudinaryUrl == null || cloudinaryUrl.trim().isEmpty()) {
            throw new IllegalArgumentException(
                "Cloudinary URL is required when storage.provider=cloudinary. " +
                "Set CLOUDINARY_URL environment variable."
            );
        }

        // Trim whitespace (Railway might add spaces)
        String trimmedUrl = cloudinaryUrl.trim();

        // WORKAROUND: Railway sometimes includes the variable name in the value
        // Remove "CLOUDINARY_URL=" prefix if present
        if (trimmedUrl.startsWith("CLOUDINARY_URL=")) {
            log.warn("Detected CLOUDINARY_URL prefix in value. Stripping it. Check Railway variable configuration.");
            trimmedUrl = trimmedUrl.substring("CLOUDINARY_URL=".length());
            log.info("URL after stripping prefix: [{}]", trimmedUrl);
        }

        if (!trimmedUrl.startsWith("cloudinary://")) {
            log.error("Invalid Cloudinary URL format. Received: [{}]", trimmedUrl);
            log.error("URL bytes: {}", trimmedUrl.getBytes());
            throw new IllegalArgumentException(
                "Invalid Cloudinary URL format. Expected: cloudinary://api_key:api_secret@cloud_name. " +
                "Received: " + trimmedUrl.substring(0, Math.min(20, trimmedUrl.length())) + "..."
            );
        }

        log.info("Initializing Cloudinary client from URL: cloudinary://***:***@{}",
            extractCloudName(trimmedUrl));

        Cloudinary cloudinary = new Cloudinary(trimmedUrl);

        // Verify configuration by extracting cloud name
        String cloudName = cloudinary.config.cloudName;
        if (cloudName == null || cloudName.trim().isEmpty()) {
            throw new IllegalArgumentException(
                "Failed to parse Cloudinary URL. Cloud name is missing."
            );
        }

        log.info("Cloudinary configured successfully with cloud name: {}", cloudName);

        return cloudinary;
    }

    /**
     * Extracts cloud name from Cloudinary URL for logging (without exposing credentials).
     *
     * @param url Cloudinary URL
     * @return cloud name or "unknown" if parsing fails
     */
    private String extractCloudName(String url) {
        try {
            // Format: cloudinary://api_key:api_secret@cloud_name
            int atIndex = url.lastIndexOf('@');
            if (atIndex != -1 && atIndex < url.length() - 1) {
                return url.substring(atIndex + 1);
            }
        } catch (Exception e) {
            log.warn("Failed to extract cloud name from URL", e);
        }
        return "unknown";
    }
}
