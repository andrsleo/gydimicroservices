package com.affiliate.rentals.gydi.shared.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import lombok.extern.slf4j.Slf4j;

/**
 * Web MVC configuration for serving static files in development.
 *
 * <p>This configuration allows the application to serve uploaded files
 * directly from the local filesystem in development mode.</p>
 *
 * <p><b>Active only in development profile ({@code @Profile("dev")})</b></p>
 *
 * @author GYDI Development Team
 */
@Configuration
@Profile("dev")
@Slf4j
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${storage.local.directory:./uploads}")
    private String uploadDirectory;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String absolutePath = uploadDirectory.startsWith("./")
                ? System.getProperty("user.dir") + "/" + uploadDirectory.substring(2)
                : uploadDirectory;

        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + absolutePath + "/");

        log.info("Static file serving configured: /uploads/** -> {}", absolutePath);
    }
}
