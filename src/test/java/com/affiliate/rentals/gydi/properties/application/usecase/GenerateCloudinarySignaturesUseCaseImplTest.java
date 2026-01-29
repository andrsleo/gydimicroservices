package com.affiliate.rentals.gydi.properties.application.usecase;

import com.affiliate.rentals.gydi.properties.application.dto.CloudinarySignatureRequest;
import com.affiliate.rentals.gydi.properties.application.dto.CloudinarySignatureResponse;
import com.affiliate.rentals.gydi.properties.domain.model.Property;
import com.affiliate.rentals.gydi.properties.domain.model.PropertyId;
import com.affiliate.rentals.gydi.properties.domain.ports.out.PropertyRepositoryPort;
import com.affiliate.rentals.gydi.shared.infrastructure.storage.CloudinaryStorageAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GenerateCloudinarySignaturesUseCaseImplTest {

    @Mock
    private PropertyRepositoryPort propertyRepository;

    @Mock
    private CloudinaryStorageAdapter cloudinaryAdapter;

    @InjectMocks
    private GenerateCloudinarySignaturesUseCaseImpl useCase;

    @Mock
    private Property property;

    private PropertyId propertyId;
    private Long userId;

    @BeforeEach
    void setUp() {
        propertyId = PropertyId.of(123L);
        userId = 1L;
    }

    @Test
    void shouldGenerateSignaturesForImages_whenValidRequest() {
        // Given
        CloudinarySignatureRequest request = new CloudinarySignatureRequest(
                3, // 3 images
                CloudinarySignatureRequest.MediaType.IMAGE
        );

        when(propertyRepository.findById(propertyId)).thenReturn(Optional.of(property));
        when(property.isOwnedBy(userId)).thenReturn(true);

        // Mock Cloudinary signature generation with dynamic folder
        when(cloudinaryAdapter.generateUploadSignature(anyString(), anyString()))
                .thenAnswer(invocation -> {
                    String folder = invocation.getArgument(0);
                    String publicId = invocation.getArgument(1);
                    return createMockSignatureData(folder, publicId);
                });
        when(cloudinaryAdapter.getCloudName()).thenReturn("test-cloud");
        when(cloudinaryAdapter.getApiKey()).thenReturn("test-api-key");

        // When
        CloudinarySignatureResponse response = useCase.generateSignatures(propertyId, userId, request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.signatures()).hasSize(3);
        assertThat(response.cloudName()).isEqualTo("test-cloud");
        assertThat(response.apiKey()).isEqualTo("test-api-key");
        assertThat(response.uploadUrl()).contains("test-cloud");

        // Verify signature structure
        CloudinarySignatureResponse.UploadSignature signature = response.signatures().get(0);
        assertThat(signature.signature()).isEqualTo("test-signature");
        assertThat(signature.timestamp()).isPositive();
        assertThat(signature.folder()).contains("properties/" + propertyId.toString() + "/images");

        // Verify interactions
        verify(propertyRepository).findById(propertyId);
        verify(property).isOwnedBy(userId);
        verify(cloudinaryAdapter, times(3)).generateUploadSignature(anyString(), anyString());
    }

    @Test
    void shouldGenerateSignaturesForVideos_whenValidRequest() {
        // Given
        CloudinarySignatureRequest request = new CloudinarySignatureRequest(
                2, // 2 videos
                CloudinarySignatureRequest.MediaType.VIDEO
        );

        when(propertyRepository.findById(propertyId)).thenReturn(Optional.of(property));
        when(property.isOwnedBy(userId)).thenReturn(true);

        when(cloudinaryAdapter.generateUploadSignature(anyString(), anyString()))
                .thenAnswer(invocation -> {
                    String folder = invocation.getArgument(0);
                    String publicId = invocation.getArgument(1);
                    return createMockSignatureData(folder, publicId);
                });
        when(cloudinaryAdapter.getCloudName()).thenReturn("test-cloud");
        when(cloudinaryAdapter.getApiKey()).thenReturn("test-api-key");

        // When
        CloudinarySignatureResponse response = useCase.generateSignatures(propertyId, userId, request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.signatures()).hasSize(2);

        // Verify folder is for videos
        CloudinarySignatureResponse.UploadSignature signature = response.signatures().get(0);
        assertThat(signature.folder()).contains("properties/" + propertyId.toString() + "/videos");

        verify(cloudinaryAdapter, times(2)).generateUploadSignature(anyString(), anyString());
    }

    @Test
    void shouldThrowException_whenPropertyNotFound() {
        // Given
        CloudinarySignatureRequest request = new CloudinarySignatureRequest(
                1,
                CloudinarySignatureRequest.MediaType.IMAGE
        );

        when(propertyRepository.findById(propertyId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> useCase.generateSignatures(propertyId, userId, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Property not found");

        verify(cloudinaryAdapter, never()).generateUploadSignature(anyString(), anyString());
    }

    @Test
    void shouldThrowException_whenUserNotAuthorized() {
        // Given
        CloudinarySignatureRequest request = new CloudinarySignatureRequest(
                1,
                CloudinarySignatureRequest.MediaType.IMAGE
        );

        when(propertyRepository.findById(propertyId)).thenReturn(Optional.of(property));
        when(property.isOwnedBy(userId)).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> useCase.generateSignatures(propertyId, userId, request))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("not authorized");

        verify(cloudinaryAdapter, never()).generateUploadSignature(anyString(), anyString());
    }

    private Map<String, Object> createMockSignatureData(String folder, String publicId) {
        Map<String, Object> data = new HashMap<>();
        data.put("signature", "test-signature");
        data.put("timestamp", System.currentTimeMillis() / 1000L);
        data.put("folder", folder);
        data.put("public_id", publicId);
        data.put("api_key", "test-api-key");
        data.put("cloud_name", "test-cloud");
        return data;
    }
}
