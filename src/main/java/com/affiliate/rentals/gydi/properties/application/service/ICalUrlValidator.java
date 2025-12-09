package com.affiliate.rentals.gydi.properties.application.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.concurrent.TimeUnit;

/**
 * Service to validate iCal URLs before storing them.
 * Validates URL format, accessibility, and iCal content format.
 */
@Slf4j
@Service
public class ICalUrlValidator {

    private static final int TIMEOUT_SECONDS = 5;
    private static final long MAX_RESPONSE_SIZE = 1024 * 1024; // 1MB
    private static final String ICAL_BEGIN_MARKER = "BEGIN:VCALENDAR";

    /**
     * Validates an iCal URL with basic format check only.
     * Use this for quick validation without network calls.
     */
    public ICalValidationResult validateFormat(String icalUrl) {
        if (icalUrl == null || icalUrl.isBlank()) {
            return ICalValidationResult.invalid("iCal URL cannot be empty");
        }

        // Validate URL format
        try {
            URI uri = new URI(icalUrl);

            // Must be HTTPS for security
            if (!"https".equalsIgnoreCase(uri.getScheme())) {
                return ICalValidationResult.invalid("iCal URL must use HTTPS protocol");
            }

            // Validate host
            String host = uri.getHost();
            if (host == null || host.isBlank()) {
                return ICalValidationResult.invalid("Invalid URL: no host specified");
            }

            // Optionally validate that it's from Airbnb (can be removed for multi-source
            // support)
            if (!isAirbnbDomain(host)) {
                log.warn("iCal URL is not from Airbnb domain: {}", host);
                // Don't fail validation, just log warning for future multi-source support
            }

            return ICalValidationResult.valid("Valid iCal URL format");

        } catch (Exception e) {
            log.error("Invalid iCal URL format: {}", icalUrl, e);
            return ICalValidationResult.invalid("Invalid URL format: " + e.getMessage());
        }
    }

    /**
     * Validates an iCal URL by actually fetching and parsing the content.
     * Use this when user submits the form for comprehensive validation.
     */
    public ICalValidationResult validateWithFetch(String icalUrl) {
        // First validate format
        ICalValidationResult formatResult = validateFormat(icalUrl);
        if (!formatResult.isValid()) {
            return formatResult;
        }

        // Then fetch and validate content
        try {
            URL url = new URL(icalUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            // Configure connection
            connection.setRequestMethod("GET");
            connection.setConnectTimeout((int) TimeUnit.SECONDS.toMillis(TIMEOUT_SECONDS));
            connection.setReadTimeout((int) TimeUnit.SECONDS.toMillis(TIMEOUT_SECONDS));
            connection.setRequestProperty("User-Agent", "GYDI-Calendar-Sync/1.0");

            // Check response code
            int responseCode = connection.getResponseCode();
            if (responseCode != 200) {
                return ICalValidationResult.invalid(
                        "Unable to access iCal URL (HTTP " + responseCode + ")");
            }

            // Check content type
            String contentType = connection.getContentType();
            if (contentType != null && !isValidICalContentType(contentType)) {
                log.warn("Unexpected content type for iCal: {}", contentType);
            }

            // Read and validate content
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream()))) {

                StringBuilder content = new StringBuilder();
                String line;
                long totalBytes = 0;
                boolean foundBeginMarker = false;

                while ((line = reader.readLine()) != null) {
                    totalBytes += line.length() + 1; // +1 for newline

                    // Check size limit
                    if (totalBytes > MAX_RESPONSE_SIZE) {
                        return ICalValidationResult.invalid(
                                "iCal file too large (exceeds 1MB limit)");
                    }

                    content.append(line).append("\n");

                    // Check for iCal marker
                    if (!foundBeginMarker && line.trim().equals(ICAL_BEGIN_MARKER)) {
                        foundBeginMarker = true;
                        // We can stop reading once we confirm it's valid iCal
                        // to avoid reading entire large files
                        break;
                    }
                }

                if (!foundBeginMarker) {
                    return ICalValidationResult.invalid(
                            "URL does not contain valid iCal data (missing BEGIN:VCALENDAR)");
                }

                return ICalValidationResult.valid(
                        "Valid iCal URL - successfully fetched calendar data");
            }

        } catch (java.net.SocketTimeoutException e) {
            log.error("Timeout fetching iCal URL: {}", icalUrl, e);
            return ICalValidationResult.invalid(
                    "Timeout accessing iCal URL - please check the URL and try again");
        } catch (Exception e) {
            log.error("Error fetching iCal URL: {}", icalUrl, e);
            return ICalValidationResult.invalid(
                    "Unable to fetch iCal data: " + e.getMessage());
        }
    }

    /**
     * Check if domain is from Airbnb.
     */
    private boolean isAirbnbDomain(String host) {
        if (host == null)
            return false;

        String lowerHost = host.toLowerCase();
        return lowerHost.equals("airbnb.com")
                || lowerHost.startsWith("www.airbnb.com")
                || lowerHost.endsWith(".airbnb.com");
    }

    /**
     * Check if content type is valid for iCal.
     */
    private boolean isValidICalContentType(String contentType) {
        if (contentType == null)
            return true; // Allow if not specified

        String lowerContentType = contentType.toLowerCase();
        return lowerContentType.contains("text/calendar")
                || lowerContentType.contains("text/plain")
                || lowerContentType.contains("application/octet-stream");
    }

    /**
     * Result of iCal URL validation.
     */
    public static class ICalValidationResult {
        private final boolean valid;
        private final String message;

        private ICalValidationResult(boolean valid, String message) {
            this.valid = valid;
            this.message = message;
        }

        public static ICalValidationResult valid(String message) {
            return new ICalValidationResult(true, message);
        }

        public static ICalValidationResult invalid(String message) {
            return new ICalValidationResult(false, message);
        }

        public boolean isValid() {
            return valid;
        }

        public String getMessage() {
            return message;
        }

        @Override
        public String toString() {
            return "ICalValidationResult{valid=" + valid + ", message='" + message + "'}";
        }
    }
}
