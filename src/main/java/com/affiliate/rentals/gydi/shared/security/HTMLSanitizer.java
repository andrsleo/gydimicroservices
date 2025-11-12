package com.affiliate.rentals.gydi.shared.security;

import lombok.extern.slf4j.Slf4j;
import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;
import org.springframework.stereotype.Component;

/**
 * HTML Sanitization Service for XSS Prevention.
 *
 * <p>This service uses the OWASP Java HTML Sanitizer library to clean user-provided
 * HTML content and prevent Cross-Site Scripting (XSS) attacks.</p>
 *
 * <p><b>SECURITY: XSS Prevention</b></p>
 *
 * <p>Features:</p>
 * <ul>
 *   <li>Removes all HTML tags and returns plain text (most secure)</li>
 *   <li>Allows only safe formatting tags (bold, italic, links)</li>
 *   <li>Sanitizes based on configurable policies</li>
 *   <li>Protects against script injection, event handlers, and dangerous attributes</li>
 * </ul>
 *
 * @author GYDI Development Team
 * @see <a href="https://github.com/OWASP/java-html-sanitizer">OWASP Java HTML Sanitizer</a>
 */
@Slf4j
@Component
public class HTMLSanitizer {

    /**
     * Policy that strips ALL HTML tags and returns plain text only.
     * This is the most secure option for user-generated content.
     */
    private static final PolicyFactory PLAIN_TEXT_POLICY = new HtmlPolicyBuilder()
            .toFactory();

    /**
     * Policy that allows only basic safe formatting tags.
     * Permitted tags: b, i, em, strong, br, p
     * No links, no images, no scripts, no event handlers.
     */
    private static final PolicyFactory BASIC_FORMATTING_POLICY = new HtmlPolicyBuilder()
            .allowElements("b", "i", "em", "strong", "br", "p", "ul", "ol", "li")
            .toFactory();

    /**
     * Policy that allows rich text with links.
     * Permitted: basic formatting + links
     * Links are sanitized to allow only http, https, and mailto protocols.
     */
    private static final PolicyFactory RICH_TEXT_POLICY = new HtmlPolicyBuilder()
            .allowElements("b", "i", "em", "strong", "br", "p", "ul", "ol", "li", "a", "blockquote")
            .allowAttributes("href").onElements("a")
            .allowStandardUrlProtocols()
            .requireRelNofollowOnLinks()  // SEO protection against spam links
            .toFactory();

    /**
     * Sanitizes HTML by stripping ALL tags and returning plain text.
     * This is the safest and recommended approach for most user content.
     *
     * <p><b>Use this for:</b></p>
     * <ul>
     *   <li>User bios</li>
     *   <li>Property descriptions</li>
     *   <li>Comments</li>
     *   <li>Any user-generated text that doesn't need formatting</li>
     * </ul>
     *
     * @param html the potentially unsafe HTML input
     * @return plain text with all HTML tags removed
     */
    public String sanitizeToPlainText(String html) {
        if (html == null || html.isBlank()) {
            return html;
        }

        String sanitized = PLAIN_TEXT_POLICY.sanitize(html);

        // Log if content was modified (potential XSS attempt)
        if (!html.equals(sanitized)) {
            log.warn("SECURITY: HTML content was sanitized. Original length: {}, Sanitized length: {}",
                    html.length(), sanitized.length());
        }

        return sanitized;
    }

    /**
     * Sanitizes HTML allowing only basic safe formatting tags.
     * Permits: b, i, em, strong, br, p, ul, ol, li
     *
     * <p><b>Use this for:</b></p>
     * <ul>
     *   <li>Blog posts (if you need basic formatting)</li>
     *   <li>Messages (if formatting is needed)</li>
     * </ul>
     *
     * @param html the potentially unsafe HTML input
     * @return sanitized HTML with only safe formatting tags
     */
    public String sanitizeBasicFormatting(String html) {
        if (html == null || html.isBlank()) {
            return html;
        }

        String sanitized = BASIC_FORMATTING_POLICY.sanitize(html);

        // Log if content was modified
        if (!html.equals(sanitized)) {
            log.warn("SECURITY: HTML content was sanitized (basic formatting). " +
                    "Original length: {}, Sanitized length: {}", html.length(), sanitized.length());
        }

        return sanitized;
    }

    /**
     * Sanitizes HTML allowing rich text with links.
     * Permits: basic formatting + links (href with http/https/mailto only)
     *
     * <p><b>Use this for:</b></p>
     * <ul>
     *   <li>Rich text editors</li>
     *   <li>Content where links are explicitly allowed</li>
     * </ul>
     *
     * <p><b>Security features:</b></p>
     * <ul>
     *   <li>Only http, https, and mailto protocols allowed</li>
     *   <li>rel="nofollow" automatically added to links (SEO protection)</li>
     *   <li>JavaScript URLs blocked (javascript:, data:, etc.)</li>
     *   <li>Event handlers removed (onclick, onload, etc.)</li>
     * </ul>
     *
     * @param html the potentially unsafe HTML input
     * @return sanitized HTML with safe formatting and links
     */
    public String sanitizeRichText(String html) {
        if (html == null || html.isBlank()) {
            return html;
        }

        String sanitized = RICH_TEXT_POLICY.sanitize(html);

        // Log if content was modified
        if (!html.equals(sanitized)) {
            log.warn("SECURITY: HTML content was sanitized (rich text). " +
                    "Original length: {}, Sanitized length: {}", html.length(), sanitized.length());
        }

        return sanitized;
    }

    /**
     * Checks if the HTML content contains potentially dangerous elements.
     *
     * @param html the HTML to check
     * @return true if the content would be modified by sanitization (contains unsafe elements)
     */
    public boolean containsUnsafeHTML(String html) {
        if (html == null || html.isBlank()) {
            return false;
        }

        String sanitized = PLAIN_TEXT_POLICY.sanitize(html);
        return !html.equals(sanitized);
    }

    /**
     * Escapes HTML special characters to prevent XSS.
     * Converts: &lt; &gt; &amp; " '
     *
     * <p>Use this when you want to display user input AS-IS but safely.</p>
     *
     * @param text the text to escape
     * @return HTML-escaped text
     */
    public String escapeHTML(String text) {
        if (text == null || text.isBlank()) {
            return text;
        }

        return text
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#x27;");
    }
}