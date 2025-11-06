package com.affiliate.rentals.gydi.shared.infrastructure.storage;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.affiliate.rentals.gydi.shared.domain.port.StoragePort;

import lombok.extern.slf4j.Slf4j;

/**
 * In-memory implementation of StoragePort for testing.
 *
 * <p>Stores file metadata in memory without actual I/O operations.
 * Perfect for fast unit and integration tests.</p>
 *
 * <p><b>Active only in test profile ({@code @Profile("test")})</b></p>
 *
 * @author GYDI Development Team
 */
@Service
@Profile("test")
@org.springframework.context.annotation.Primary
@Slf4j
public class InMemoryStorageAdapter implements StoragePort {

    private final Map<String, byte[]> storage = new ConcurrentHashMap<>();
    private final String baseUrl = "http://test.localhost/uploads";

    @Override
    public String uploadFile(MultipartFile file, String folder) {
        validateFile(file);

        String fileName = generateUniqueFileName(file.getOriginalFilename());
        String key = folder + "/" + fileName;
        String fileUrl = baseUrl + "/" + key;

        try {
            storage.put(key, file.getBytes());
            log.info("File stored in memory: {}", fileUrl);
            return fileUrl;
        } catch (IOException e) {
            log.error("Failed to read file bytes", e);
            throw new StorageException("Failed to store file in memory", e);
        }
    }

    @Override
    public void deleteFileByUrl(String fileUrl) {
        if (fileUrl == null || fileUrl.isBlank()) {
            return;
        }
        String key = extractKeyFromUrl(fileUrl);
        deleteFile(key);
    }

    @Override
    public void deleteFile(String key) {
        storage.remove(key);
        log.info("File removed from memory: {}", key);
    }

    @Override
    public String getBaseUrl() {
        return baseUrl;
    }

    public boolean fileExists(String key) {
        return storage.containsKey(key);
    }

    public byte[] getFileContent(String key) {
        return storage.get(key);
    }

    public void clear() {
        storage.clear();
        log.info("In-memory storage cleared");
    }

    private String extractKeyFromUrl(String fileUrl) {
        return fileUrl.replace(baseUrl + "/", "");
    }

    private String generateUniqueFileName(String originalFilename) {
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        return UUID.randomUUID().toString() + extension;
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new StorageException("File is empty or null");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new StorageException("Only image files are allowed");
        }
        long maxSize = 10 * 1024 * 1024;
        if (file.getSize() > maxSize) {
            throw new StorageException("File size exceeds maximum allowed size of 10MB");
        }
    }
}
