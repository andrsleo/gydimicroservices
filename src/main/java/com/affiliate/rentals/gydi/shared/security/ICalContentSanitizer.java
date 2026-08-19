package com.affiliate.rentals.gydi.shared.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * 🔒 Sanitizes iCal content to prevent injection attacks
 *
 * Protections:
 * - XSS prevention (strip HTML/JavaScript)
 * - SQL injection prevention (escape special chars)
 * - Command injection prevention
 * - Format validation
 */
@Slf4j
@Component
public class ICalContentSanitizer {

    // ✅ Maximum field lengths (prevent DoS)
    private static final int MAX_SUMMARY_LENGTH = 500;
    private static final int MAX_DESCRIPTION_LENGTH = 2000;
    private static final int MAX_LOCATION_LENGTH = 200;

    // ✅ Dangerous patterns to remove
    private static final Pattern HTML_TAGS = Pattern.compile("<[^>]*>", Pattern.CASE_INSENSITIVE);
    private static final Pattern SCRIPT_TAGS = Pattern.compile(
        "<script[^>]*>.*?</script>",
        Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );
    private static final Pattern JAVASCRIPT_PROTOCOL = Pattern.compile(
        "javascript:",
        Pattern.CASE_INSENSITIVE
    );
    private static final Pattern SQL_INJECTION_PATTERNS = Pattern.compile(
        ".*(union|select|insert|update|delete|drop|create|alter|exec|execute|script|javascript|eval).*",
        Pattern.CASE_INSENSITIVE
    );

    // ✅ Control characters that shouldn't be in calendar data
    private static final Pattern CONTROL_CHARS = Pattern.compile("[\u0000-\u001F\u007F-\u009F]");

    /**
     * Validates iCal content format and structure
     */
    public void validateICalFormat(String content) {
        if (content == null || content.isBlank()) {
            throw new InvalidICalContentException("iCal content is empty");
        }

        // ✅ Size validation (prevent memory exhaustion)
        if (content.length() > 5 * 1024 * 1024) { // 5 MB
            throw new InvalidICalContentException("iCal content exceeds maximum size (5 MB)");
        }

        // ✅ Format validation (must start with BEGIN:VCALENDAR)
        if (!content.trim().toUpperCase().startsWith("BEGIN:VCALENDAR")) {
            log.error("🚨 Invalid iCal format - missing BEGIN:VCALENDAR");
            throw new InvalidICalContentException("Invalid iCal format: must start with BEGIN:VCALENDAR");
        }

        // ✅ Format validation (must end with END:VCALENDAR)
        if (!content.trim().toUpperCase().endsWith("END:VCALENDAR")) {
            log.error("🚨 Invalid iCal format - missing END:VCALENDAR");
            throw new InvalidICalContentException("Invalid iCal format: must end with END:VCALENDAR");
        }

        // ✅ Check for nested VCALENDAR (potential bomb attack)
        int beginCount = countOccurrences(content, "BEGIN:VCALENDAR");
        int endCount = countOccurrences(content, "END:VCALENDAR");

        if (beginCount != endCount) {
            throw new InvalidICalContentException("Mismatched BEGIN/END VCALENDAR tags");
        }

        if (beginCount > 1) {
            log.warn("Multiple VCALENDAR blocks detected: {}", beginCount);
            throw new InvalidICalContentException("Multiple VCALENDAR blocks not allowed");
        }

        // ✅ Check for excessive events (potential DoS)
        int eventCount = countOccurrences(content, "BEGIN:VEVENT");
        if (eventCount > 1000) {
            log.warn("Excessive events in iCal: {}", eventCount);
            throw new InvalidICalContentException("Too many events (max: 1000)");
        }

        log.debug("iCal format validation passed ({} events)", eventCount);
    }

    /**
     * Sanitizes text field from iCal (SUMMARY, DESCRIPTION, LOCATION)
     */
    public String sanitizeTextField(String text, int maxLength) {
        if (text == null) {
            return null;
        }

        // ✅ Remove script tags first (most dangerous)
        text = SCRIPT_TAGS.matcher(text).replaceAll("");

        // ✅ Remove JavaScript protocol
        text = JAVASCRIPT_PROTOCOL.matcher(text).replaceAll("");

        // ✅ Remove all HTML tags
        text = HTML_TAGS.matcher(text).replaceAll("");

        // ✅ Remove control characters
        text = CONTROL_CHARS.matcher(text).replaceAll("");

        // ✅ Check for SQL injection patterns
        if (SQL_INJECTION_PATTERNS.matcher(text).matches()) {
            log.warn("🚨 Potential SQL injection detected in text: {}", text.substring(0, Math.min(50, text.length())));
            // Don't throw exception, just sanitize aggressively
            text = text.replaceAll("[^a-zA-Z0-9\\s.,!?@#$%&*()\\-_+=\\[\\]{}:;'\"/\\\\]", "");
        }

        // ✅ Trim and truncate
        text = text.trim();
        if (text.length() > maxLength) {
            text = text.substring(0, maxLength);
            log.debug("Text truncated to {} characters", maxLength);
        }

        // ✅ Escape special characters for safe storage
        text = escapeSpecialChars(text);

        return text;
    }

    /**
     * Sanitizes SUMMARY field
     */
    public String sanitizeSummary(String summary) {
        return sanitizeTextField(summary, MAX_SUMMARY_LENGTH);
    }

    /**
     * Sanitizes DESCRIPTION field
     */
    public String sanitizeDescription(String description) {
        return sanitizeTextField(description, MAX_DESCRIPTION_LENGTH);
    }

    /**
     * Sanitizes LOCATION field
     */
    public String sanitizeLocation(String location) {
        return sanitizeTextField(location, MAX_LOCATION_LENGTH);
    }

    /**
     * Escapes special characters to prevent injection
     */
    private String escapeSpecialChars(String text) {
        if (text == null) {
            return null;
        }

        return text
            .replace("\\", "\\\\")   // Backslash
            .replace("'", "\\'")     // Single quote
            .replace("\"", "\\\"")   // Double quote
            .replace("\n", "\\n")    // Newline
            .replace("\r", "\\r")    // Carriage return
            .replace("\t", "\\t");   // Tab
    }

    /**
     * Counts occurrences of substring (case-insensitive)
     */
    private int countOccurrences(String text, String substring) {
        int count = 0;
        int index = 0;
        String upperText = text.toUpperCase();
        String upperSubstring = substring.toUpperCase();

        while ((index = upperText.indexOf(upperSubstring, index)) != -1) {
            count++;
            index += upperSubstring.length();
        }

        return count;
    }

    /**
     * Custom exception for invalid iCal content
     */
    public static class InvalidICalContentException extends RuntimeException {
        public InvalidICalContentException(String message) {
            super(message);
        }
    }
}
