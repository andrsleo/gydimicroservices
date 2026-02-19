package com.affiliate.rentals.gydi.properties.application.usecase;

import com.affiliate.rentals.gydi.properties.application.dto.SaveUploadedMediaCommand;
import com.affiliate.rentals.gydi.properties.domain.model.Property;
import com.affiliate.rentals.gydi.properties.domain.model.PropertyId;
import com.affiliate.rentals.gydi.properties.domain.ports.out.PropertyRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SaveUploadedMediaUseCaseImplTest {

    @Mock
    private PropertyRepositoryPort propertyRepository;

    @InjectMocks
    private SaveUploadedMediaUseCaseImpl useCase;

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
    void shouldSaveUploadedImages_whenValidUrls() {
        // Given
        List<SaveUploadedMediaCommand.MediaUrl> mediaUrls = List.of(
                new SaveUploadedMediaCommand.MediaUrl(
                        "https://res.cloudinary.com/test/image/upload/v123/property/img1.jpg",
                        0,
                        null
                ),
                new SaveUploadedMediaCommand.MediaUrl(
                        "https://res.cloudinary.com/test/image/upload/v123/property/img2.jpg",
                        1,
                        null
                )
        );
        SaveUploadedMediaCommand command = new SaveUploadedMediaCommand(mediaUrls);

        when(propertyRepository.findById(propertyId)).thenReturn(Optional.of(property));
        when(property.isOwnedBy(userId)).thenReturn(true);
        when(propertyRepository.save(property)).thenReturn(property);

        // When
        Property result = useCase.saveUploadedImages(propertyId, userId, command);

        // Then
        assertThat(result).isNotNull();
        verify(property, times(2)).addImage(anyString(), anyInt());
        verify(propertyRepository).save(property);
    }

    @Test
    void shouldSaveUploadedVideos_whenValidUrls() {
        // Given
        List<SaveUploadedMediaCommand.MediaUrl> mediaUrls = List.of(
                new SaveUploadedMediaCommand.MediaUrl(
                        "https://res.cloudinary.com/test/video/upload/v123/property/video1.mp4",
                        0,
                        null
                )
        );
        SaveUploadedMediaCommand command = new SaveUploadedMediaCommand(mediaUrls);

        when(propertyRepository.findById(propertyId)).thenReturn(Optional.of(property));
        when(property.isOwnedBy(userId)).thenReturn(true);
        when(propertyRepository.save(property)).thenReturn(property);

        // When
        Property result = useCase.saveUploadedVideos(propertyId, userId, command);

        // Then
        assertThat(result).isNotNull();
        verify(property).addVideo(
                eq("https://res.cloudinary.com/test/video/upload/v123/property/video1.mp4"),
                eq("https://res.cloudinary.com/test/video/upload/so_0/v123/property/video1.jpg"),
                eq(0),
                isNull()
        );
        verify(propertyRepository).save(property);
    }

    @Test
    void shouldThrowException_whenPropertyNotFound() {
        // Given
        SaveUploadedMediaCommand command = new SaveUploadedMediaCommand(List.of(
                new SaveUploadedMediaCommand.MediaUrl(
                        "https://res.cloudinary.com/test/image/upload/v123/img.jpg",
                        0,
                        null
                )
        ));

        when(propertyRepository.findById(propertyId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> useCase.saveUploadedImages(propertyId, userId, command))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Property not found");

        verify(property, never()).addImage(anyString(), anyInt());
        verify(propertyRepository, never()).save(any());
    }

    @Test
    void shouldThrowException_whenUserNotAuthorized() {
        // Given
        SaveUploadedMediaCommand command = new SaveUploadedMediaCommand(List.of(
                new SaveUploadedMediaCommand.MediaUrl(
                        "https://res.cloudinary.com/test/image/upload/v123/img.jpg",
                        0,
                        null
                )
        ));

        when(propertyRepository.findById(propertyId)).thenReturn(Optional.of(property));
        when(property.isOwnedBy(userId)).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> useCase.saveUploadedImages(propertyId, userId, command))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("not authorized");

        verify(property, never()).addImage(anyString(), anyInt());
        verify(propertyRepository, never()).save(any());
    }

    @Test
    void shouldThrowException_whenInvalidCloudinaryUrl() {
        // Given
        SaveUploadedMediaCommand command = new SaveUploadedMediaCommand(List.of(
                new SaveUploadedMediaCommand.MediaUrl(
                        "https://evil.com/malicious/file.jpg", // Not a Cloudinary URL
                        0,
                        null
                )
        ));

        when(propertyRepository.findById(propertyId)).thenReturn(Optional.of(property));
        when(property.isOwnedBy(userId)).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> useCase.saveUploadedImages(propertyId, userId, command))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid media URL");

        verify(property, never()).addImage(anyString(), anyInt());
        verify(propertyRepository, never()).save(any());
    }

    @Test
    void shouldGenerateCorrectThumbnailUrl_forMp4Video() {
        // Given
        SaveUploadedMediaCommand command = new SaveUploadedMediaCommand(List.of(
                new SaveUploadedMediaCommand.MediaUrl(
                        "https://res.cloudinary.com/test/video/upload/v123/property/video.mp4",
                        0,
                        null
                )
        ));

        when(propertyRepository.findById(propertyId)).thenReturn(Optional.of(property));
        when(property.isOwnedBy(userId)).thenReturn(true);
        when(propertyRepository.save(property)).thenReturn(property);

        // When
        useCase.saveUploadedVideos(propertyId, userId, command);

        // Then
        verify(property).addVideo(
                eq("https://res.cloudinary.com/test/video/upload/v123/property/video.mp4"),
                eq("https://res.cloudinary.com/test/video/upload/so_0/v123/property/video.jpg"), // Thumbnail with so_0
                eq(0),
                isNull()
        );
    }
}
