package com.affiliate.rentals.gydi.shared.infrastructure.storage;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.affiliate.rentals.gydi.shared.domain.port.StoragePort;
import com.affiliate.rentals.gydi.shared.security.FileValidator;
import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;

import lombok.extern.slf4j.Slf4j;

/**
 * Cloudinary implementation of StoragePort for production file storage.
 *
 * <p>
 * Cloudinary provides:
 * <ul>
 * <li><b>CDN</b> - Global content delivery network for fast image/video
 * loading</li>
 * <li><b>Automatic optimization</b> - Format conversion (WebP), quality
 * optimization, compression</li>
 * <li><b>Transformations</b> - On-the-fly image resizing, cropping,
 * effects</li>
 * <li><b>Responsive images</b> - Generate multiple sizes for different
 * devices</li>
 * <li><b>Video streaming</b> - Adaptive bitrate streaming for videos</li>
 * <li><b>Persistence</b> - Files never lost (unlike ephemeral container
 * storage)</li>
 * </ul>
 * </p>
 *
 * <p>
 * <b>Active when:</b> {@code storage.provider=cloudinary} and NOT in test
 * profile
 * </p>
 *
 * <p>
 * <b>Configuration in application.yml:</b>
 * 
 * <pre>
 * cloudinary:
 *   url: cloudinary://api_key:api_secret@cloud_name
 *
 * storage:
 *   provider: cloudinary  # Activates this adapter
 * </pre>
 * </p>
 *
 * <p>
 * <b>Environment Variables (Railway/Vercel):</b>
 * 
 * <pre>
 * CLOUDINARY_URL=cloudinary://595781714461924:1VW549K9XIyLrBlco4mJYcWu4NU@cloudestoragegydi
 * STORAGE_PROVIDER=cloudinary
 * </pre>
 * </p>
 *
 * <p>
 * <b>Upload Flow:</b>
 * <ol>
 * <li>Frontend sends file to backend</li>
 * <li>Backend validates file (type, size, magic numbers)</li>
 * <li>Backend uploads to Cloudinary via this adapter</li>
 * <li>Cloudinary returns public URL with CDN</li>
 * <li>Backend stores URL in database</li>
 * <li>Frontend displays image from Cloudinary CDN</li>
 * </ol>
 * </p>
 *
 * <p>
 * <b>Example URLs:</b>
 * <ul>
 * <li>Original:
 * {@code https://res.cloudinary.com/cloudestoragegydi/image/upload/v1234/property-images/uuid.jpg}</li>
 * <li>Optimized:
 * {@code ...image/upload/q_auto,f_auto/v1234/property-images/uuid.jpg}</li>
 * <li>Resized:
 * {@code ...image/upload/w_800,h_600,c_fill/v1234/property-images/uuid.jpg}</li>
 * </ul>
 * </p>
 *
 * @see com.affiliate.rentals.gydi.shared.config.CloudinaryConfig
 * @author GYDI Development Team
 */
@Slf4j
@Service
@Primary
@Profile("!test")
@ConditionalOnProperty(name = "storage.provider", havingValue = "cloudinary")
public class CloudinaryStorageAdapter implements StoragePort {

    private final Cloudinary cloudinary;
    private final FileValidator fileValidator;
    private final String cloudName;

    public CloudinaryStorageAdapter(Cloudinary cloudinary, FileValidator fileValidator) {
        this.cloudinary = cloudinary;
        this.fileValidator = fileValidator;
        this.cloudName = cloudinary.config.cloudName;

        log.info("CloudinaryStorageAdapter initialized with cloud name: {}", cloudName);
    }

    /**
     * Upload file to Cloudinary.
     *
     * <p>
     * Cloudinary automatically:
     * <ul>
     * <li>Optimizes image quality</li>
     * <li>Converts to modern formats (WebP) when requested</li>
     * <li>Generates multiple responsive sizes</li>
     * <li>Applies CDN caching</li>
     * <li>Generates unique public_id to prevent collisions</li>
     * </ul>
     * </p>
     *
     * @param file   the file to upload
     * @param folder the folder/prefix (e.g., "property-images", "profile-images")
     * @return the public CDN URL of the uploaded file
     * @throws StorageException if upload fails
     */
    @Override
    public String uploadFile(MultipartFile file, String folder) {
        // SECURITY: Validate file before upload
        validateFile(file);

        String originalFilename = file.getOriginalFilename();
        String fileName = generateUniqueFileName(originalFilename);
        // NOTE: Only use fileName as public_id, NOT folder/fileName
        // Cloudinary will automatically prepend the folder from the "folder" option
        String publicId = fileName;

        try {
            // Prepare upload options
            Map<String, Object> uploadOptions = new HashMap<>();
            uploadOptions.put("public_id", publicId);
            uploadOptions.put("folder", folder); // Cloudinary prepends this to public_id
            uploadOptions.put("resource_type", "auto"); // Auto-detect: image or video
            uploadOptions.put("overwrite", false); // Never overwrite existing files
            uploadOptions.put("unique_filename", true); // Cloudinary ensures uniqueness

            // Determine resource type for better optimization
            String contentType = file.getContentType();
            if (contentType != null) {
                if (contentType.startsWith("video/")) {
                    uploadOptions.put("resource_type", "video");
                    // Video-specific optimizations
                    // Note: eager transformations are processed asynchronously by Cloudinary
                    // Using List format as required by Cloudinary SDK
                    uploadOptions.put("eager", Arrays.asList(
                            ObjectUtils.asMap("streaming_profile", "hd")
                    ));
                    uploadOptions.put("eager_async", true); // Process asynchronously
                } else if (contentType.startsWith("image/")) {
                    uploadOptions.put("resource_type", "image");
                    // Image-specific optimizations
                    uploadOptions.put("quality", "auto:good"); // Automatic quality optimization
                    uploadOptions.put("fetch_format", "auto"); // Auto WebP conversion
                }
            }

            // Upload to Cloudinary
            log.debug("Uploading file to Cloudinary: folder={}, fileName={}, size={}",
                    folder, fileName, file.getSize());

            @SuppressWarnings("unchecked")
            Map<String, Object> uploadResult = cloudinary.uploader().upload(
                    file.getBytes(),
                    uploadOptions);

            // Extract public URL
            String secureUrl = (String) uploadResult.get("secure_url");
            String publicIdResult = (String) uploadResult.get("public_id");

            log.info("File uploaded successfully to Cloudinary: publicId={}, url={}",
                    publicIdResult, secureUrl);

            return secureUrl;

        } catch (IOException e) {
            log.error("Failed to upload file to Cloudinary: fileName={}, folder={}",
                    originalFilename, folder, e);
            throw new StorageException("Failed to upload file to Cloudinary: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Unexpected error uploading to Cloudinary: fileName={}, folder={}",
                    originalFilename, folder, e);
            throw new StorageException("Unexpected error uploading to Cloudinary: " + e.getMessage(), e);
        }
    }

    /**
     * Delete file from Cloudinary using its URL.
     *
     * <p>
     * Extracts the public_id from the URL and deletes the resource.
     * </p>
     *
     * @param fileUrl the Cloudinary URL of the file
     */
    @Override
    public void deleteFileByUrl(String fileUrl) {
        if (fileUrl == null || fileUrl.isBlank()) {
            log.warn("Attempted to delete file with null or empty URL");
            return;
        }

        try {
            String publicId = extractPublicIdFromUrl(fileUrl);
            if (publicId == null) {
                log.warn("Could not extract public_id from URL: {}", fileUrl);
                return;
            }

            deleteFile(publicId);

        } catch (Exception e) {
            log.error("Failed to delete file by URL: {}", fileUrl, e);
            // Don't throw exception - deletion is best-effort
        }
    }

    /**
     * Delete file from Cloudinary using its public_id.
     *
     * @param publicId the Cloudinary public_id (e.g., "property-images/uuid")
     */
    @Override
    public void deleteFile(String publicId) {
        if (publicId == null || publicId.isBlank()) {
            log.warn("Attempted to delete file with null or empty public_id");
            return;
        }

        try {
            log.debug("Deleting file from Cloudinary: publicId={}", publicId);

            // Determine resource type from public_id or try both
            Map<String, Object> deleteResult = null;

            // Try as image first
            try {
                deleteResult = cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
            } catch (Exception e) {
                // If image deletion fails, try as video
                log.debug("Image deletion failed, trying as video: publicId={}", publicId);
                Map<String, Object> videoOptions = new HashMap<>();
                videoOptions.put("resource_type", "video");
                deleteResult = cloudinary.uploader().destroy(publicId, videoOptions);
            }

            String result = (String) deleteResult.get("result");
            if ("ok".equals(result)) {
                log.info("File deleted successfully from Cloudinary: publicId={}", publicId);
            } else if ("not found".equals(result)) {
                log.warn("File not found in Cloudinary: publicId={}", publicId);
            } else {
                log.warn("Unexpected deletion result from Cloudinary: result={}, publicId={}",
                        result, publicId);
            }

        } catch (Exception e) {
            log.error("Failed to delete file from Cloudinary: publicId={}", publicId, e);
            // Don't throw exception - deletion is best-effort
        }
    }

    /**
     * Get the base URL for Cloudinary CDN.
     *
     * @return the Cloudinary CDN base URL
     */
    @Override
    public String getBaseUrl() {
        return String.format("https://res.cloudinary.com/%s", cloudName);
    }

    /**
     * Extract Cloudinary public_id from a Cloudinary URL.
     *
     * <p>
     * Example URL:
     * {@code https://res.cloudinary.com/cloudestoragegydi/image/upload/v1234567890/property-images/uuid.jpg}
     * </p>
     *
     * <p>
     * Extracted public_id:
     * {@code property-images/uuid}
     * </p>
     *
     * @param url the Cloudinary URL
     * @return the public_id without version and extension, or null if extraction
     *         fails
     */
    private String extractPublicIdFromUrl(String url) {
        try {
            // URL format:
            // https://res.cloudinary.com/{cloud_name}/{resource_type}/upload/v{version}/{public_id}.{format}
            // Example:
            // https://res.cloudinary.com/cloudestoragegydi/image/upload/v1234567890/property-images/uuid.jpg

            if (!url.contains("res.cloudinary.com")) {
                log.warn("URL is not a Cloudinary URL: {}", url);
                return null;
            }

            // Split by "/upload/"
            String[] parts = url.split("/upload/");
            if (parts.length < 2) {
                log.warn("Invalid Cloudinary URL format (no /upload/): {}", url);
                return null;
            }

            // After "/upload/": v1234567890/property-images/uuid.jpg
            String afterUpload = parts[1];

            // Remove version: property-images/uuid.jpg
            String withoutVersion = afterUpload.replaceFirst("^v\\d+/", "");

            // Remove file extension: property-images/uuid
            String publicId = withoutVersion.replaceFirst("\\.[^.]+$", "");

            log.debug("Extracted public_id from URL: {} -> {}", url, publicId);
            return publicId;

        } catch (Exception e) {
            log.error("Failed to extract public_id from URL: {}", url, e);
            return null;
        }
    }

    /**
     * Generate a unique filename to avoid collisions.
     *
     * @param originalFilename the original filename
     * @return a unique filename with UUID
     */
    private String generateUniqueFileName(String originalFilename) {
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        // Remove extension for Cloudinary (it handles formats automatically)
        return UUID.randomUUID().toString();
    }

    /**
     * Validate file before upload using FileValidator.
     *
     * <p>
     * SECURITY: This validates:
     * <ul>
     * <li>File size limits (10MB images, 500MB videos)</li>
     * <li>File type (MIME type)</li>
     * <li>Magic number verification (prevents MIME spoofing)</li>
     * <li>Malicious content detection</li>
     * </ul>
     * </p>
     *
     * @param file the file to validate
     * @throws StorageException if validation fails
     */
    private void validateFile(MultipartFile file) {
        String contentType = file.getContentType();

        if (contentType != null && contentType.startsWith("video/")) {
            fileValidator.validateVideo(file);
        } else {
            fileValidator.validateImage(file);
        }
    }

    /**
     * Generate signature for direct Cloudinary upload.
     *
     * <p>
     * This allows the frontend to upload files directly to Cloudinary without
     * going through the backend, which:
     * <ul>
     * <li>Reduces backend load</li>
     * <li>Enables parallel uploads (faster)</li>
     * <li>Supports larger files (no backend timeout)</li>
     * <li>Provides per-file progress tracking</li>
     * </ul>
     * </p>
     *
     * <p><b>Security:</b> Signature is time-limited (1 hour) and signed with API secret.</p>
     *
     * @param folder the Cloudinary folder (e.g., "properties/123/images")
     * @param publicId unique identifier for this upload
     * @return map containing signature, timestamp, and other params
     */
    public Map<String, Object> generateUploadSignature(String folder, String publicId) {
        try {
            // Generate timestamp (valid for 1 hour)
            long timestamp = System.currentTimeMillis() / 1000L;

            // Prepare parameters for signature.
            // IMPORTANT: Only include params that the frontend will send as form fields.
            // resource_type is part of the URL path (/auto/upload), NOT a signed parameter.
            Map<String, Object> params = new HashMap<>();
            params.put("timestamp", timestamp);
            params.put("folder", folder);
            params.put("public_id", publicId);

            // Generate signature using Cloudinary's algorithm
            String signature = cloudinary.apiSignRequest(params, cloudinary.config.apiSecret);

            // Return signed parameters for frontend
            Map<String, Object> result = new HashMap<>();
            result.put("signature", signature);
            result.put("timestamp", timestamp);
            result.put("folder", folder);
            result.put("public_id", publicId);
            result.put("api_key", cloudinary.config.apiKey);
            result.put("cloud_name", cloudinary.config.cloudName);

            log.debug("Generated upload signature: folder={}, publicId={}, timestamp={}",
                    folder, publicId, timestamp);

            return result;

        } catch (Exception e) {
            log.error("Failed to generate upload signature: folder={}, publicId={}",
                    folder, publicId, e);
            throw new StorageException("Failed to generate upload signature: " + e.getMessage(), e);
        }
    }

    /**
     * Get API key for Cloudinary (used by frontend for direct uploads).
     *
     * @return the Cloudinary API key
     */
    public String getApiKey() {
        return cloudinary.config.apiKey;
    }

    /**
     * Get cloud name for Cloudinary (used by frontend for direct uploads).
     *
     * @return the Cloudinary cloud name
     */
    public String getCloudName() {
        return cloudinary.config.cloudName;
    }
}
