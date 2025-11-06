package com.affiliate.rentals.gydi.shared.infrastructure.storage;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.affiliate.rentals.gydi.shared.domain.port.StoragePort;

import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

/**
 * AWS S3 implementation of StoragePort for production use.
 *
 * <p>This adapter handles uploading profile images and other assets to AWS S3,
 * generating public URLs, and deleting files when necessary.</p>
 *
 * <p><b>Active only in production profile ({@code @Profile("prod")})</b></p>
 *
 * <p><b>Configuration required in application-prod.yml:</b></p>
 * <pre>
 * aws:
 *   s3:
 *     bucket-name: your-bucket-name
 *     region: us-east-1
 *     access-key: ${AWS_ACCESS_KEY_ID}
 *     secret-key: ${AWS_SECRET_ACCESS_KEY}
 *     base-url: https://your-bucket.s3.amazonaws.com
 * </pre>
 *
 * @author GYDI Development Team
 */
@Service
@Profile("prod")
@Slf4j
public class S3StorageService implements StoragePort {

    private final S3Client s3Client;
    private final String bucketName;
    private final String baseUrl;

    public S3StorageService(
            @Value("${aws.s3.bucket-name}") String bucketName,
            @Value("${aws.s3.region}") String region,
            @Value("${aws.s3.access-key}") String accessKey,
            @Value("${aws.s3.secret-key}") String secretKey,
            @Value("${aws.s3.base-url}") String baseUrl) {
        this.bucketName = bucketName;
        this.baseUrl = baseUrl;

        AwsBasicCredentials awsCreds = AwsBasicCredentials.create(accessKey, secretKey);
        this.s3Client = S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(awsCreds))
                .build();

        log.info("S3StorageService initialized for bucket: {} in region: {}", bucketName, region);
    }

    @Override
    public String uploadFile(MultipartFile file, String folder) {
        validateFile(file);

        String fileName = generateUniqueFileName(file.getOriginalFilename());
        String key = folder + "/" + fileName;

        try (InputStream inputStream = file.getInputStream()) {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .contentType(file.getContentType())
                    .contentLength(file.getSize())
                    .build();

            s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(inputStream, file.getSize()));

            String fileUrl = baseUrl + "/" + key;
            log.info("File uploaded successfully to S3: {}", fileUrl);
            return fileUrl;

        } catch (IOException e) {
            log.error("Failed to read file: {}", file.getOriginalFilename(), e);
            throw new StorageException("Failed to read file", e);
        } catch (S3Exception e) {
            log.error("Failed to upload file to S3: {}", key, e);
            throw new StorageException("Failed to upload file to S3", e);
        }
    }

    @Override
    public void deleteFileByUrl(String fileUrl) {
        if (fileUrl == null || fileUrl.isBlank()) {
            return;
        }

        try {
            String key = extractKeyFromUrl(fileUrl);
            deleteFile(key);
        } catch (Exception e) {
            log.error("Failed to delete file from S3: {}", fileUrl, e);
        }
    }

    @Override
    public void deleteFile(String key) {
        try {
            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            s3Client.deleteObject(deleteObjectRequest);
            log.info("File deleted successfully from S3: {}", key);

        } catch (S3Exception e) {
            log.error("Failed to delete file from S3: {}", key, e);
            throw new StorageException("Failed to delete file from S3", e);
        }
    }

    @Override
    public String getBaseUrl() {
        return baseUrl;
    }

    /**
     * Extract S3 key from full URL.
     *
     * @param fileUrl the full S3 URL
     * @return the S3 key (path after bucket name)
     */
    private String extractKeyFromUrl(String fileUrl) {
        // Format: https://bucket-name.s3.amazonaws.com/folder/filename.ext
        // or: https://your-custom-domain.com/folder/filename.ext
        String urlWithoutProtocol = fileUrl.replaceFirst("^https?://[^/]+/", "");
        return urlWithoutProtocol;
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
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new StorageException("Only image files are allowed");
        }

        // Max file size: 10MB
        long maxSize = 10 * 1024 * 1024;
        if (file.getSize() > maxSize) {
            throw new StorageException("File size exceeds maximum allowed size of 10MB");
        }
    }
}
