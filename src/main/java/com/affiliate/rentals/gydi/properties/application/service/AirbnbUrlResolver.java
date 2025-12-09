package com.affiliate.rentals.gydi.properties.application.service;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URL;
import java.util.List;

/**
 * Service responsible for resolving Airbnb short URLs (e.g. abnb.me, /h/ paths)
 * to their canonical /rooms/ format.
 */
@Component
public class AirbnbUrlResolver {

    private static final Logger log = LoggerFactory.getLogger(AirbnbUrlResolver.class);
    private static final int TIMEOUT_MILLIS = 10000;
    private static final String USER_AGENT = "Mozilla/5.0 (compatible; GydiBot/1.0)";

    private static final List<String> ALLOWED_HOSTS = List.of(
            "airbnb.com", "www.airbnb.com",
            "abnb.me", "www.abnb.me",
            "airbnb.es", "www.airbnb.es",
            "airbnb.mx", "www.airbnb.mx",
            "airbnb.ca", "www.airbnb.ca",
            "airbnb.co.uk", "www.airbnb.co.uk");

    /**
     * Resolves a potential short/mobile URL to its canonical form.
     * If the URL is already canonical or resolution fails, returns the original
     * URL.
     */
    public String resolve(String url) {
        if (url == null || url.isBlank())
            return url;

        // Only attempt resolution for known short domains or paths
        if (!needsResolution(url)) {
            return url;
        }

        log.info("Resolving potential short Airbnb URL: {}", url);

        try {
            Connection.Response response = Jsoup.connect(url)
                    .userAgent(USER_AGENT)
                    .timeout(TIMEOUT_MILLIS)
                    .followRedirects(true)
                    .method(Connection.Method.HEAD)
                    .execute();

            URL finalUrl = response.url();
            String resolvedUrl = finalUrl.toString();

            // Verify we stayed on a valid domain
            if (!isValidHost(finalUrl.getHost())) {
                log.warn("URL resolution redirected to invalid host: {}", finalUrl.getHost());
                return url; // Return original to let validator reject it
            }

            // Remove query parameters from resolved URL to clean it up
            int queryIndex = resolvedUrl.indexOf('?');
            if (queryIndex > 0) {
                resolvedUrl = resolvedUrl.substring(0, queryIndex);
            }

            log.info("Resolved URL to: {}", resolvedUrl);
            return resolvedUrl;

        } catch (IOException e) {
            log.warn("Failed to resolve URL {}: {}", url, e.getMessage());
            return url; // Return original on error
        }
    }

    private boolean needsResolution(String url) {
        return url.contains("abnb.me") || url.contains("/h/");
    }

    private boolean isValidHost(String host) {
        if (host == null)
            return false;
        String lowerHost = host.toLowerCase();
        return ALLOWED_HOSTS.contains(lowerHost);
    }
}
