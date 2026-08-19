package com.affiliate.rentals.gydi.shared.security;

import java.io.IOException;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

/**
 * Security filter for static resources (uploaded files).
 * Adds security headers to prevent execution of malicious files and XSS
 * attacks.
 *
 * <p>
 * <b>Security Headers Applied:</b>
 * </p>
 * <ul>
 * <li><b>X-Content-Type-Options: nosniff</b> - Prevents MIME type sniffing (XSS
 * protection)</li>
 * <li><b>X-Frame-Options: DENY</b> - Prevents clickjacking attacks</li>
 * <li><b>Content-Security-Policy</b> - Prevents script execution from uploaded
 * files</li>
 * <li><b>Cache-Control</b> - Configures caching for performance</li>
 * </ul>
 *
 * <p>
 * This filter is automatically registered to intercept requests to
 * {@code /uploads/*}
 * </p>
 *
 * @author GYDI Development Team
 * @see SecurityConfig
 */
@Component
@Slf4j
public class StaticResourceSecurityFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response,
            FilterChain chain) throws IOException, ServletException {

        HttpServletResponse httpResponse = (HttpServletResponse) response;

        // SECURITY: Prevent MIME type sniffing (XSS protection)
        // Ensures browsers respect the Content-Type header
        httpResponse.setHeader("X-Content-Type-Options", "nosniff");

        // SECURITY: Prevent clickjacking attacks
        // Prevents uploaded images from being embedded in iframes on malicious sites
        httpResponse.setHeader("X-Frame-Options", "DENY");

        // SECURITY: Content Security Policy for static files
        // Prevents scripts from executing even if malicious files bypass validation
        httpResponse.setHeader("Content-Security-Policy",
                "default-src 'none'; img-src 'self'; style-src 'none'; script-src 'none';");

        // PERFORMANCE: Cache static files for 1 hour
        // Adjust based on your needs (e.g., 'private, no-cache' for sensitive content)
        httpResponse.setHeader("Cache-Control", "public, max-age=3600");

        // SECURITY: Prevent browsers from storing sensitive content
        // Uncomment for sensitive files (e.g., private user documents)
        // httpResponse.setHeader("Cache-Control", "private, no-cache, no-store,
        // must-revalidate");
        // httpResponse.setHeader("Pragma", "no-cache");
        // httpResponse.setHeader("Expires", "0");

        // log.debug("Security headers applied to static resource request"); //
        // Commented to reduce log noise

        chain.doFilter(request, response);
    }

    /**
     * Registers this filter to intercept requests to /uploads/*
     *
     * @param filter the StaticResourceSecurityFilter instance
     * @return FilterRegistrationBean configured for /uploads/* URLs
     */
    @Bean
    public FilterRegistrationBean<StaticResourceSecurityFilter> staticResourceFilterRegistration(
            StaticResourceSecurityFilter filter) {
        FilterRegistrationBean<StaticResourceSecurityFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(filter);
        registration.addUrlPatterns("/uploads/*");
        registration.setOrder(1); // Execute early in the filter chain
        registration.setName("StaticResourceSecurityFilter");
        log.info("StaticResourceSecurityFilter registered for /uploads/* URLs");
        return registration;
    }
}