package com.affiliate.rentals.gydi.shared.infrastructure.storage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.affiliate.rentals.gydi.shared.domain.port.StoragePort;

import lombok.extern.slf4j.Slf4j;

/**
 * Local filesystem implementation of StoragePort for development.
 *
 * <p>Stores files in a local directory (default: {@code ./uploads/})
 * and returns URLs like {@code http://localhost:8080/uploads/profile-images/uuid.jpg}</p>
 *
 * <p><b>Active only in development profile ({@code @Profile("dev")})</b></p>
 *
 * <p><b>Benefits:</b></p>
 * <ul>
 *   <li>No AWS credentials needed</li>
 *   <li>No AWS costs</li>
 *   <li>Easy to inspect uploaded files</li>
 *   <li>Fast local I/O</li>
 * </ul>
 *
 * <p><b>Configuration in application-dev.yml:</b></p>
 * <pre>
 * storage:
 *   local:
 *     directory: ./uploads
 *     base-url: http://localhost:8080/uploads
 * </pre>
 *
 * @author GYDI Development Team
 */
@Service
@Profile("dev")
@org.springframework.context.annotation.Primary
@Slf4j
public class LocalFileSystemStorageAdapter implements StoragePort {

    private final Path uploadDirectory;
    private final String baseUrl;

    public LocalFileSystemStorageAdapter(
            @Value("${storage.local.directory:./uploads}") String uploadDirectory,
            @Value("${storage.local.base-url:http://localhost:8080/uploads}") String baseUrl) {

        this.uploadDirectory = Paths.get(uploadDirectory).toAbsolutePath().normalize();
        this.baseUrl = baseUrl;

        try {
            Files.createDirectories(this.uploadDirectory);
            log.info("LocalFileSystemStorageAdapter initialized. Upload directory: {}", this.uploadDirectory);
        } catch (IOException e) {
            throw new StorageException("Could not create upload directory", e);
        }
    }

    @Override
    public String uploadFile(MultipartFile file, String folder) {
        validateFile(file);

        String fileName = generateUniqueFileName(file.getOriginalFilename());
        String relativePath = folder + "/" + fileName;

        try {
            Path folderPath = uploadDirectory.resolve(folder);
            Files.createDirectories(folderPath);

            Path targetLocation = uploadDirectory.resolve(relativePath);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            String fileUrl = baseUrl + "/" + relativePath;
            log.info("File uploaded to local filesystem: {}", fileUrl);
            return fileUrl;

        } catch (IOException e) {
            log.error("Failed to store file: {}", fileName, e);
            throw new StorageException("Failed to store file", e);
        }
    }

    @Override
    public void deleteFileByUrl(String fileUrl) {
        if (fileUrl == null || fileUrl.isBlank()) {
            return;
        }

        try {
            String relativePath = extractRelativePathFromUrl(fileUrl);
            deleteFile(relativePath);
        } catch (Exception e) {
            log.error("Failed to delete file: {}", fileUrl, e);
        }
    }

    @Override
    public void deleteFile(String relativePath) {
        try {
            Path filePath = uploadDirectory.resolve(relativePath).normalize();

            // Security check: ensure file is within upload directory
            if (!filePath.startsWith(uploadDirectory)) {
                throw new StorageException("Cannot delete file outside upload directory");
            }

            Files.deleteIfExists(filePath);
            log.info("File deleted from local filesystem: {}", relativePath);

        } catch (IOException e) {
            log.error("Failed to delete file: {}", relativePath, e);
            throw new StorageException("Failed to delete file", e);
        }
    }

    @Override
    public String getBaseUrl() {
        return baseUrl;
    }

    /**
     * Extract relative path from URL.
     *
     * @param fileUrl the full URL
     * @return the relative path
     */
    private String extractRelativePathFromUrl(String fileUrl) {
        // Remove base URL to get relative path
        return fileUrl.replace(baseUrl + "/", "");
    }

    /**
     * Generate a unique filename to avoid collisions.
     *
     * @param originalFilename the original filename
     * @return a unique filename with UUID prefix
     */
    private String generateUniqueFileName(String originalFilename) {
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        return UUID.randomUUID().toString() + extension;
    }

    /**
     * Validate file before upload.
     *
     * @param file the file to validate
     * @throws StorageException if validation fails
     */
    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new StorageException("File is empty or null");
        }

        String contentType = file.getContentType();
        if (contentType == null || (!contentType.startsWith("image/") && !contentType.startsWith("video/"))) {
            throw new StorageException("Only image and video files are allowed");
        }

        // Max file size: 500MB (for videos)
        long maxSize = 500 * 1024 * 1024;
        if (file.getSize() > maxSize) {
            throw new StorageException("File size exceeds maximum allowed size of 500MB");
        }
    }
}
