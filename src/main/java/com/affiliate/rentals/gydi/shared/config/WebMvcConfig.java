package com.affiliate.rentals.gydi.shared.config;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

import lombok.extern.slf4j.Slf4j;

/**
 * Web MVC configuration for serving static files in development.
 *
 * <p>
 * This configuration allows the application to serve uploaded files
 * directly from the local filesystem in development mode.
 * </p>
 *
 * <p>
 * <b>Active in development and local profiles
 * ({@code @Profile({"dev", "local"})})</b>
 * </p>
 *
 * @author GYDI Development Team
 */
@Configuration
@Profile({ "dev", "local" })
@Slf4j
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${storage.local.directory:./uploads}")
    private String uploadDirectory;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String absolutePath = uploadDirectory.startsWith("./")
                ? System.getProperty("user.dir") + "/" + uploadDirectory.substring(2)
                : uploadDirectory;

        Path uploadPath = Paths.get(absolutePath).toAbsolutePath().normalize();

        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + absolutePath + "/")
                .setCachePeriod(3600) // 1 hour cache for performance
                .resourceChain(true)
                .addResolver(new PathResourceResolver() {
                    @Override
                    protected Resource getResource(String resourcePath, Resource location)
                            throws IOException {
                        Resource resource = super.getResource(resourcePath, location);

                        if (resource != null && resource.exists() && resource.isFile()) {
                            // SECURITY: Verify file is within allowed upload directory
                            // Prevents path traversal attacks (e.g., /uploads/../../../etc/passwd)
                            Path filePath = resource.getFile().toPath()
                                    .toAbsolutePath().normalize();

                            if (!filePath.startsWith(uploadPath)) {
                                log.warn("SECURITY: Path traversal attempt blocked: {}", resourcePath);
                                return null;
                            }

                            return resource;
                        }

                        // SECURITY: Block directory listing
                        return null;
                    }
                });

        log.info("Static file serving configured: /uploads/** -> {}", absolutePath);
        log.info("Path traversal protection enabled for upload directory: {}", uploadPath);
    }
}
