package com.affiliate.rentals.gydi.shared.security;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import com.affiliate.rentals.gydi.shared.infrastructure.storage.StorageException;

import lombok.extern.slf4j.Slf4j;

/**
 * Validates uploaded files for security vulnerabilities.
 *
 * <p>Performs comprehensive validation including:
 * <ul>
 *   <li><b>Magic number validation</b>: Verifies file signature (first bytes) matches declared type</li>
 *   <li><b>Extension whitelist</b>: Only allows safe file extensions</li>
 *   <li><b>Content-Type validation</b>: Checks MIME type header</li>
 *   <li><b>Size limits</b>: Enforces maximum file size per type</li>
 * </ul>
 *
 * <p><b>Security Benefits:</b></p>
 * <ul>
 *   <li>Prevents malicious file uploads (JSP, PHP, executables)</li>
 *   <li>Prevents MIME type spoofing attacks</li>
 *   <li>Prevents path traversal via filename manipulation</li>
 *   <li>Prevents DoS via large file uploads</li>
 * </ul>
 *
 * <p><b>Example Usage:</b></p>
 * <pre>{@code
 * @Service
 * public class PropertyImageService {
 *     private final FileValidator fileValidator;
 *
 *     public void uploadImage(MultipartFile file) {
 *         fileValidator.validateImage(file);
 *         // Proceed with upload
 *     }
 * }
 * }</pre>
 *
 * @author GYDI Security Team
 * @version 1.0
 * @since 2025-11-10
 * @see <a href="https://en.wikipedia.org/wiki/List_of_file_signatures">File Signatures</a>
 */
@Slf4j
@Component
public class FileValidator {
    

    /**
     * Magic numbers (file signatures) for supported image formats.
     * Key: MIME type
     * Value: Magic number bytes (file must start with these bytes)
     *
     * Note: HEIC/HEIF/AVIF use ISO Base Media File Format (ISOBMFF).
     * Their magic number check is handled separately in {@link #hasValidMagicNumber}
     * because the ftyp box size (first 4 bytes) can vary.
     */
    private static final Map<String, byte[]> IMAGE_MAGIC_NUMBERS = Map.of(
        "image/jpeg", new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF},
        "image/png", new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A},
        "image/gif", new byte[]{0x47, 0x49, 0x46, 0x38}, // "GIF8"
        "image/webp", new byte[]{0x52, 0x49, 0x46, 0x46}, // "RIFF"
        "image/bmp", new byte[]{0x42, 0x4D} // "BM"
    );

    /**
     * MIME types that use ISO Base Media File Format (ftyp box at offset 4).
     * These require special magic number validation since the box size varies.
     */
    private static final Set<String> ISOBMFF_IMAGE_TYPES = Set.of(
        "image/heic", "image/heif", "image/avif"
    );

    /**
     * Magic numbers for video formats.
     */
    private static final Map<String, byte[]> VIDEO_MAGIC_NUMBERS = Map.of(
        "video/mp4", new byte[]{0x00, 0x00, 0x00, 0x18, 0x66, 0x74, 0x79, 0x70}, // ftyp
        "video/quicktime", new byte[]{0x00, 0x00, 0x00, 0x14, 0x66, 0x74, 0x79, 0x70}, // ftyp
        "video/x-msvideo", new byte[]{0x52, 0x49, 0x46, 0x46}, // "RIFF" (AVI)
        "video/webm", new byte[]{0x1A, 0x45, (byte) 0xDF, (byte) 0xA3} // WebM signature
    );

    /**
     * Allowed file extensions (whitelist).
     * SECURITY: Only safe extensions are allowed. Dangerous extensions like .jsp, .jspx, .war are blocked.
     */
    private static final Set<String> ALLOWED_IMAGE_EXTENSIONS = Set.of(
        ".jpg", ".jpeg", ".png", ".gif", ".webp", ".bmp",
        ".heic", ".heif", ".avif"
    );

    private static final Set<String> ALLOWED_VIDEO_EXTENSIONS = Set.of(
        ".mp4", ".mov", ".avi", ".webm"
    );

    /**
     * File size limits (in bytes).
     */
    private static final long MAX_IMAGE_SIZE = 10 * 1024 * 1024; // 10MB
    private static final long MAX_VIDEO_SIZE = 100 * 1024 * 1024; // 100MB

    /**
     * Validates an uploaded image file.
     *
     * @param file the uploaded file to validate
     * @throws StorageException if validation fails
     */
    public void validateImage(MultipartFile file) {
        validateFile(file, IMAGE_MAGIC_NUMBERS, ALLOWED_IMAGE_EXTENSIONS, MAX_IMAGE_SIZE, "image");
    }

    /**
     * Validates an uploaded video file.
     *
     * @param file the uploaded file to validate
     * @throws StorageException if validation fails
     */
    public void validateVideo(MultipartFile file) {
        validateFile(file, VIDEO_MAGIC_NUMBERS, ALLOWED_VIDEO_EXTENSIONS, MAX_VIDEO_SIZE, "video");
    }

    /**
     * Generic file validation method.
     *
     * @param file the uploaded file
     * @param magicNumbers map of MIME type to magic number bytes
     * @param allowedExtensions set of allowed file extensions
     * @param maxSize maximum file size in bytes
     * @param fileType type of file (for error messages)
     * @throws StorageException if validation fails
     */
    private void validateFile(
        MultipartFile file,
        Map<String, byte[]> magicNumbers,
        Set<String> allowedExtensions,
        long maxSize,
        String fileType
    ) {
        // 1. Check file is not empty
        if (file == null || file.isEmpty()) {
            log.warn("File upload rejected: file is empty or null");
            throw new StorageException("File is empty or null");
        }

        String originalFilename = file.getOriginalFilename();
        String contentType = file.getContentType();

        log.debug("Validating {} file: filename={}, contentType={}, size={}",
            fileType, originalFilename, contentType, file.getSize());

        // 2. Validate filename exists
        if (originalFilename == null || originalFilename.trim().isEmpty()) {
            log.warn("File upload rejected: filename is null or empty");
            throw new StorageException("Filename is required");
        }

        // 3. Validate file extension (whitelist)
        if (!hasAllowedExtension(originalFilename, allowedExtensions)) {
            log.warn("File upload rejected: invalid extension. Filename: {}", originalFilename);
            throw new StorageException(
                String.format("Invalid file extension. Allowed extensions: %s",
                    String.join(", ", allowedExtensions))
            );
        }

        // 4. Validate Content-Type
        if (contentType == null || !contentType.startsWith(fileType + "/")) {
            log.warn("File upload rejected: invalid content type. ContentType: {}", contentType);
            throw new StorageException(
                String.format("Invalid content type. Expected %s/* but got: %s", fileType, contentType)
            );
        }

        // 5. Validate file size
        if (file.getSize() > maxSize) {
            log.warn("File upload rejected: size exceeds limit. Size: {}, Max: {}",
                file.getSize(), maxSize);
            throw new StorageException(
                String.format("File size exceeds maximum allowed size of %d MB",
                    maxSize / 1024 / 1024)
            );
        }

        // 6. Validate magic numbers (file signature)
        try {
            byte[] fileBytes = file.getBytes();
            if (!hasValidMagicNumber(fileBytes, contentType, magicNumbers)) {
                log.warn("File upload rejected: magic number validation failed. " +
                    "Filename: {}, ContentType: {}", originalFilename, contentType);
                throw new StorageException(
                    "File content does not match declared type. Possible file type spoofing detected."
                );
            }
        } catch (IOException e) {
            log.error("Failed to read file bytes for validation", e);
            throw new StorageException("Failed to read file for validation");
        }

        // 7. Check for path traversal attempts in filename
        if (containsPathTraversal(originalFilename)) {
            log.warn("File upload rejected: path traversal attempt detected. Filename: {}",
                originalFilename);
            throw new StorageException("Invalid filename: path traversal detected");
        }

        // 8. Check for null bytes in filename (potential security issue)
        if (originalFilename.contains("\0")) {
            log.warn("File upload rejected: null byte in filename. Filename: {}", originalFilename);
            throw new StorageException("Invalid filename: contains null bytes");
        }

        log.info("File validation successful: filename={}, size={}", originalFilename, file.getSize());
    }

    /**
     * Checks if the filename has an allowed extension.
     *
     * @param filename the filename to check
     * @param allowedExtensions set of allowed extensions
     * @return true if extension is allowed, false otherwise
     */
    private boolean hasAllowedExtension(String filename, Set<String> allowedExtensions) {
        if (filename == null || !filename.contains(".")) {
            return false;
        }

        String extension = filename.substring(filename.lastIndexOf('.')).toLowerCase();
        return allowedExtensions.contains(extension);
    }

    /**
     * Validates the file's magic number (file signature).
     *
     * <p>Magic numbers are the first few bytes of a file that identify the file type.
     * This prevents attackers from uploading malicious files with fake extensions or MIME types.</p>
     *
     * @param fileBytes the file's byte content
     * @param contentType the declared MIME type
     * @param magicNumbers map of MIME types to their magic numbers
     * @return true if magic number matches, false otherwise
     */
    private boolean hasValidMagicNumber(
        byte[] fileBytes,
        String contentType,
        Map<String, byte[]> magicNumbers
    ) {
        if (fileBytes == null || fileBytes.length < 8) {
            return false; // File too small to have a valid magic number
        }

        // HEIC/HEIF/AVIF use ISO Base Media File Format with "ftyp" at offset 4
        if (ISOBMFF_IMAGE_TYPES.contains(contentType)) {
            return hasIsobmffFtypBox(fileBytes);
        }

        byte[] expectedMagicNumber = magicNumbers.get(contentType);
        if (expectedMagicNumber == null) {
            log.warn("No magic number defined for content type: {}", contentType);
            return false; // Content type not in our whitelist
        }

        // Check if file starts with the expected magic number
        for (int i = 0; i < expectedMagicNumber.length && i < fileBytes.length; i++) {
            if (fileBytes[i] != expectedMagicNumber[i]) {
                log.debug("Magic number mismatch at byte {}: expected {}, got {}",
                    i, expectedMagicNumber[i], fileBytes[i]);
                return false;
            }
        }

        return true;
    }

    /**
     * Checks for ISO Base Media File Format "ftyp" box at offset 4.
     * HEIC, HEIF, and AVIF files all use this container format.
     * The first 4 bytes are the box size (variable), bytes 4-7 must be "ftyp".
     */
    private boolean hasIsobmffFtypBox(byte[] fileBytes) {
        if (fileBytes.length < 12) {
            return false;
        }
        // Bytes 4-7 must be "ftyp" (0x66 0x74 0x79 0x70)
        return fileBytes[4] == 0x66
            && fileBytes[5] == 0x74
            && fileBytes[6] == 0x79
            && fileBytes[7] == 0x70;
    }

    /**
     * Checks if the filename contains path traversal patterns.
     *
     * @param filename the filename to check
     * @return true if path traversal detected, false otherwise
     */
    private boolean containsPathTraversal(String filename) {
        return filename.contains("../") ||
               filename.contains("..\\") ||
               filename.contains("/..") ||
               filename.contains("\\..") ||
               filename.startsWith("/") ||
               filename.startsWith("\\");
    }

    /**
     * Sanitizes a filename by removing dangerous characters.
     * Use this after validation to ensure safe filenames for storage.
     *
     * @param filename the original filename
     * @return sanitized filename
     */
    public String sanitizeFilename(String filename) {
        if (filename == null || filename.trim().isEmpty()) {
            return "unnamed_file";
        }

        // Remove path components
        String sanitized = filename.replaceAll("[\\\\/]", "");

        // Remove special characters except dot, dash, underscore
        sanitized = sanitized.replaceAll("[^a-zA-Z0-9._-]", "_");

        // Remove multiple consecutive dots
        sanitized = sanitized.replaceAll("\\.{2,}", ".");

        // Remove leading/trailing dots or dashes
        sanitized = sanitized.replaceAll("^[.-]+|[.-]+$", "");

        // Ensure filename is not empty after sanitization
        if (sanitized.isEmpty()) {
            sanitized = "unnamed_file";
        }

        return sanitized;
    }
}
