package com.affiliate.rentals.gydi.properties.infrastructure.out.storage;

import com.affiliate.rentals.gydi.properties.domain.ports.out.PropertyStoragePort;
import com.affiliate.rentals.gydi.shared.domain.port.StoragePort;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

@Component
public class PropertyStorageAdapter implements PropertyStoragePort {

    private final StoragePort storagePort;

    public PropertyStorageAdapter(StoragePort storagePort) {
        this.storagePort = storagePort;
    }

    @Override
    public String uploadPropertyImage(String propertyId, String fileName, String contentType, byte[] data) {
        MultipartFile file = createMultipartFile(fileName, contentType, data);
        return storagePort.uploadFile(file, "properties/" + propertyId + "/images");
    }

    @Override
    public String uploadPropertyVideo(String propertyId, String fileName, String contentType, byte[] data) {
        MultipartFile file = createMultipartFile(fileName, contentType, data);
        return storagePort.uploadFile(file, "properties/" + propertyId + "/videos");
    }

    private MultipartFile createMultipartFile(String name, String contentType, byte[] content) {
        return new MultipartFile() {
            @Override
            public String getName() { return name; }

            @Override
            public String getOriginalFilename() { return name; }

            @Override
            public String getContentType() { return contentType; }

            @Override
            public boolean isEmpty() { return content == null || content.length == 0; }

            @Override
            public long getSize() { return content != null ? content.length : 0; }

            @Override
            public byte[] getBytes() { return content; }

            @Override
            public InputStream getInputStream() { return new ByteArrayInputStream(content); }

            @Override
            public void transferTo(java.io.File dest) throws IOException, IllegalStateException {
                throw new UnsupportedOperationException("Not implemented");
            }
        };
    }

    @Override
    public String generateAndUploadVideoThumbnail(String videoUrl, String propertyId) {
        return videoUrl
            .replace(".mp4", "_thumb.jpg")
            .replace(".webm", "_thumb.jpg")
            .replace(".mov", "_thumb.jpg")
            .replace(".MOV", "_thumb.jpg");
    }

    @Override
    public void deletePropertyImage(String imageUrl) {
        storagePort.deleteFileByUrl(imageUrl);
    }

    @Override
    public void deletePropertyVideo(String videoUrl, String thumbnailUrl) {
        storagePort.deleteFileByUrl(videoUrl);
        if (thumbnailUrl != null) storagePort.deleteFileByUrl(thumbnailUrl);
    }

    @Override
    public String uploadFile(MultipartFile file, String folder) {
        return storagePort.uploadFile(file, folder);
    }

    @Override
    public void deleteFileByUrl(String fileUrl) {
        storagePort.deleteFileByUrl(fileUrl);
    }

    @Override
    public void deleteFile(String key) {
        storagePort.deleteFile(key);
    }

    @Override
    public String getBaseUrl() {
        return storagePort.getBaseUrl();
    }
}
