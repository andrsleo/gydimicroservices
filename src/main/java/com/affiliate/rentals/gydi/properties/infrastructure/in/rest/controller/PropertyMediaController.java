package com.affiliate.rentals.gydi.properties.infrastructure.in.rest.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.affiliate.rentals.gydi.properties.application.dto.PropertyResponse;
import com.affiliate.rentals.gydi.properties.application.mapper.PropertyMapper;
import com.affiliate.rentals.gydi.properties.domain.model.Property;
import com.affiliate.rentals.gydi.properties.domain.model.PropertyId;
import com.affiliate.rentals.gydi.properties.domain.ports.in.UploadPropertyImagesUseCase;
import com.affiliate.rentals.gydi.properties.domain.ports.in.UploadPropertyVideosUseCase;
import com.affiliate.rentals.gydi.shared.security.JwtService;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/properties/{propertyId}/media")
public class PropertyMediaController {

    private final UploadPropertyImagesUseCase uploadImagesUseCase;
    private final UploadPropertyVideosUseCase uploadVideosUseCase;
    private final PropertyMapper mapper;
    private final JwtService jwtService;

    public PropertyMediaController(UploadPropertyImagesUseCase uploadImagesUseCase,
                                  UploadPropertyVideosUseCase uploadVideosUseCase,
                                  PropertyMapper mapper,
                                  JwtService jwtService) {
        this.uploadImagesUseCase = uploadImagesUseCase;
        this.uploadVideosUseCase = uploadVideosUseCase;
        this.mapper = mapper;
        this.jwtService = jwtService;
    }

    @PostMapping("/images")
    public ResponseEntity<PropertyResponse> uploadImages(
            @PathVariable String propertyId,
            @RequestParam("files") MultipartFile[] files,
            HttpServletRequest httpRequest) throws IOException {

        Long userId = jwtService.extractUserIdFromRequest(httpRequest);

        List<UploadPropertyImagesUseCase.ImageUpload> images = new ArrayList<>();
        for (int i = 0; i < files.length; i++) {
            MultipartFile file = files[i];
            images.add(new UploadPropertyImagesUseCase.ImageUpload(
                file.getOriginalFilename(),
                file.getContentType(),
                file.getSize(),
                file.getBytes(),
                -1  // Let the domain layer handle displayOrder
            ));
        }

        Property property = uploadImagesUseCase.uploadImages(
            new UploadPropertyImagesUseCase.UploadImagesCommand(
                PropertyId.of(propertyId),
                userId,
                images
            )
        );

        return ResponseEntity.ok(mapper.toPropertyResponse(property));
    }

    @PostMapping("/videos")
    public ResponseEntity<PropertyResponse> uploadVideos(
            @PathVariable String propertyId,
            @RequestParam("files") MultipartFile[] files,
            HttpServletRequest httpRequest) throws IOException {

        Long userId = jwtService.extractUserIdFromRequest(httpRequest);

        List<UploadPropertyVideosUseCase.VideoUpload> videos = new ArrayList<>();
        for (int i = 0; i < files.length; i++) {
            MultipartFile file = files[i];
            videos.add(new UploadPropertyVideosUseCase.VideoUpload(
                file.getOriginalFilename(),
                file.getContentType(),
                file.getSize(),
                file.getBytes(),
                -1  // Let the domain layer handle displayOrder
            ));
        }

        Property property = uploadVideosUseCase.uploadVideos(
            new UploadPropertyVideosUseCase.UploadVideosCommand(
                PropertyId.of(propertyId),
                userId,
                videos
            )
        );

        return ResponseEntity.ok(mapper.toPropertyResponse(property));
    }
}
