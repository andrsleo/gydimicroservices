package com.affiliate.rentals.gydi.shared.domain.port;

import org.springframework.web.multipart.MultipartFile;

/**
 * Port interface for file storage operations.
 *
 * <p>This port abstracts file storage allowing different implementations:
 * <ul>
 *   <li><b>AWS S3</b> for production ({@code S3StorageAdapter})</li>
 *   <li><b>Local filesystem</b> for development ({@code LocalFileSystemStorageAdapter})</li>
 *   <li><b>In-memory</b> for testing ({@code InMemoryStorageAdapter})</li>
 * </ul>
 * </p>
 *
 * <p>This follows the <b>Hexagonal Architecture</b> pattern, where the domain layer
 * defines the port (interface) and the infrastructure layer provides adapters (implementations).</p>
 *
 * @author GYDI Development Team
 */
public interface StoragePort {

    /**
     * Upload a file to storage and return the public URL.
     *
     * @param file the file to upload
     * @param folder the folder/prefix in storage (e.g., "profile-images", "property-photos")
     * @return the public URL of the uploaded file
     * @throws com.affiliate.rentals.gydi.shared.infrastructure.storage.StorageException if upload fails
     */
    String uploadFile(MultipartFile file, String folder);

    /**
     * Delete a file from storage using its URL.
     *
     * @param fileUrl the public URL of the file to delete
     */
    void deleteFileByUrl(String fileUrl);

    /**
     * Delete a file from storage using its key/path.
     *
     * @param key the storage key (path) of the file
     */
    void deleteFile(String key);

    /**
     * Get the base URL for file access.
     *
     * <p>Example:</p>
     * <ul>
     *   <li>S3: {@code https://bucket.s3.amazonaws.com}</li>
     *   <li>Local: {@code http://localhost:8080/uploads}</li>
     *   <li>Test: {@code http://test.localhost/uploads}</li>
     * </ul>
     *
     * @return the base URL
     */
    String getBaseUrl();
}
